package org.iota.qupla.qupla.context;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.ConcatExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.IntegerExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.TypeExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;

public class QuplaEvalContext extends QuplaBaseContext
{
  private static final boolean allowLog = false;
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();
  private static final boolean useBreak = true;
  private static final boolean usePrint = true;
  private static final boolean varNamesOnStack = true;

  public int callNr;
  public byte[] callTrail = new byte[4096];
  public final Stack<TritVector> stack = new Stack<>();
  public int stackFrame;
  public TritVector value;

  public QuplaEvalContext()
  {
  }

  public void createEntityEffects(final FuncStmt func)
  {
    final Entity entity = new Entity(func, 1);
    entity.queueEffectEvents(value);
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    assign.expr.eval(this);
    log("     " + assign.name + " = ", stack.peek(), assign.expr);

    if (varNamesOnStack)
    {
      value = new TritVector(value);
      value.name = assign.name;
    }

    stack.push(value);

    // is this actually an assignment to a state variable?
    if (assign.stateIndex == 0)
    {
      // nope, done
      return;
    }

    //  only assign non-null trits, other trits remain the same
    if (value.isNull())
    {
      // all null, just don't assign anything
      return;
    }

    // save index of state variable to be able to distinguish
    // between multiple state vars in the same function
    // assuming no more than about 250 state variables here
    callTrail[callNr] = (byte) assign.stateIndex;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;
    final StateValue stateValue = stateValues.get(call);

    // overwrite all trits?
    if (!value.isValue())
    {
      assign.warning("Partially overwriting state");

      // get existing state or all zero default state
      final TritVector trits = stateValue != null ? stateValue.value : new TritVector(value.size(), '0');
      final char[] buffer = new char[trits.size()];
      for (int i = 0; i < value.size(); i++)
      {
        // only overwrite non-null trits
        final char trit = value.trit(i);
        buffer[i] = trit == '@' ? trits.trit(i) : trit;
      }

      // use the merged result as the value to set the state to
      value = new TritVector(new String(buffer));
    }

    // state already saved?
    if (stateValue != null)
    {
      // reset state?
      if (value.isZero())
      {
        stateValues.remove(call);
        return;
      }

      // overwrite state
      stateValue.value = value;
      return;
    }

    // state not saved yet

    // reset state?
    if (value.isZero())
    {
      // already reset
      return;
    }

    // save state
    call.path = Arrays.copyOf(callTrail, callNr + 1);
    call.value = value;
    stateValues.put(call, call);
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    concat.lhs.eval(this);
    if (concat.rhs == null)
    {
      return;
    }

    final TritVector lhsValue = value;
    concat.rhs.eval(this);
    value = TritVector.concat(lhsValue, value);
  }

  private void evalConcatExprs(final ArrayList<BaseExpr> exprs)
  {
    TritVector result = null;
    for (final BaseExpr expr : exprs)
    {
      expr.eval(this);
      result = TritVector.concat(result, value);
    }

    value = result;
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    conditional.condition.eval(this);
    if (conditional.trueBranch == null)
    {
      // not really a conditional operator
      // should have been optimized away
      return;
    }

    final char trit = value.trit(0);
    if (trit == '1')
    {
      conditional.trueBranch.eval(this);
      return;
    }

    if (conditional.falseBranch != null && trit == '-')
    {
      conditional.falseBranch.eval(this);
      return;
    }

    // a non-bool condition value will result in a null return value
    // because both nullify calls will return null
    value = new TritVector(conditional.size, '@');
  }

  public TritVector evalEntity(final Entity entity, final TritVector vector)
  {
    log("effect ", vector, entity.func);

    int start = 0;
    for (final BaseExpr param : entity.func.params)
    {
      value = vector.slicePadded(start, param.size);
      value.name = param.name;
      stack.push(value);
      start += param.size;
    }

    entity.func.eval(this);
    log("     return ", value, entity.func.returnExpr);

    stack.clear();
    return value;
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
    }

    func.returnExpr.eval(this);
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    //TODO initialize callTrail with some id to distinguish between
    //     top level functions so that we don't accidentally use the same
    //     call path from within different top level functions to store
    //     state data in the stateValues HashMap when call path is short

    if (callNr == 4000)
    {
      call.error("Exceeded function call nesting limit");
    }

    callTrail[callNr++] = (byte) call.callIndex;

    int newStackFrame = stack.size();

    if (pushArguments(call))
    {
      log("short-circuit " + call.func.name);
      value = call.func.nullReturn;
    }
    else
    {
      log("call " + call.func.name);
      int oldStackFrame = stackFrame;
      stackFrame = newStackFrame;
      call.func.eval(this);
      stackFrame = oldStackFrame;
      log("     return ", value, call.func.returnExpr);
    }

    stack.setSize(newStackFrame);
    callNr--;

    interceptCall(call);
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    evalConcatExprs(lookup.args);

    // all trits non-null?
    if (value.isValue())
    {
      final int lutIndex = LutStmt.index(value);
      value = lookup.lut.lookup[lutIndex];
      if (value != null)
      {
        return;
      }
    }

    value = lookup.lut.undefined;
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    merge.lhs.eval(this);

    // if there is no rhs we return lhs
    if (merge.rhs == null)
    {
      return;
    }

    // if lhs is null then we return rhs
    if (value.isNull())
    {
      merge.rhs.eval(this);
      return;
    }

    // if rhs is null then we return lhs
    final TritVector savedLhsBranch = value;
    merge.rhs.eval(this);
    if (value.isNull())
    {
      value = savedLhsBranch;
      return;
    }

    merge.rhs.error("Multiple non-null merge branches");
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    final TritVector var = stack.get(stackFrame + slice.stackIndex);
    if (slice.startOffset == null && slice.fields.size() == 0)
    {
      value = var;
      return;
    }

    value = var.slice(slice.start, slice.size);
  }

  @Override
  public void evalState(final StateExpr state)
  {
    // save index of state variable to be able to distinguish
    // between multiple state vars in the same function
    callTrail[callNr] = (byte) state.stackIndex;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;

    // if state was saved before set to that value otherwise set to zero
    final StateValue stateValue = stateValues.get(call);
    value = stateValue != null ? stateValue.value : state.zero;
    if (varNamesOnStack)
    {
      value = new TritVector(value);
      value.name = state.name;
    }

    stack.push(value);
    log("     state " + state.name + " = ", stack.peek(), state);
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    // type expression is a concatenation, but in declared field order
    // analyze will have sorted the fields in order already
    evalConcatExprs(type.fields);
  }

  @Override
  public void evalVector(final IntegerExpr integer)
  {
    value = integer.vector;
  }

  private void interceptCall(final FuncExpr call)
  {
    if (usePrint && call.name.startsWith("print_"))
    {
      final BaseExpr arg = call.args.get(0);
      BaseExpr.logLine("" + arg.typeInfo.display(value));
    }

    if (useBreak && call.name.startsWith("break"))
    {
      final BaseExpr arg = call.args.get(0);
      BaseExpr.logLine("" + arg.typeInfo.display(value));
    }
  }

  public void log(final String text, final TritVector vector, final BaseExpr expr)
  {
    // avoid converting vector to string, which is slow
    if (allowLog)
    {
      log(text + vector + " : " + expr);
    }
  }

  public void log(final String text)
  {
    if (allowLog)
    {
      BaseExpr.logLine(text);
    }
  }

  private boolean pushArguments(final FuncExpr call)
  {
    boolean isAllNull = true;
    for (int i = 0; i < call.args.size(); i++)
    {
      final BaseExpr arg = call.args.get(i);
      arg.eval(this);

      final boolean isNull = value.isNull();
      if (call.func.anyNull && isNull)
      {
        return true;
      }

      isAllNull = isAllNull && isNull;
      if (varNamesOnStack)
      {
        value = new TritVector(value);
        value.name = call.func.params.get(i).name;
      }

      stack.push(value);
      log("push ", value, arg);
    }

    return isAllNull;
  }
}

