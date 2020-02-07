package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.iota.qupla.Qupla;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.dispatcher.entity.FuncEntity;
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraEvalContext extends AbraBaseContext
{
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();
  private static final boolean trace = false;
  private static final TritVector tritMin = new TritVector(1, '-');
  private static final TritVector tritNull = new TritVector(1, '@');
  private static final TritVector tritOne = new TritVector(1, '1');
  private static final TritVector tritZero = new TritVector(1, '0');
  private static final boolean useBreak = true;
  private static final boolean usePrint = true;

  private final ArrayList<TritVector> args = new ArrayList<>();
  private int callNr;
  private final byte[] callTrail = new byte[4096];
  private TritVector[] stack;
  public TritVector value;

  public void eval(final QuplaToAbraContext context, final BaseExpr expr)
  {
    if (expr instanceof FuncExpr)
    {
      context.abraModule.numberBlocks();
      final FuncExpr funcExpr = (FuncExpr) expr;
      final AbraBlockBranch branch = new AbraBlockBranch();
      branch.addInputParam(1);
      context.branch = branch;
      context.evalFuncCall(funcExpr);

      final AbraSiteKnot knot = branch.sites.get(branch.sites.size() - 1);
      branch.outputs.add(knot);

      args.clear();
      args.add(new TritVector(1, '0'));
      branch.numberSites();
      callTrail[callNr++] = (byte) knot.block.index;
      callTrail[callNr++] = (byte) (knot.block.index >> 8);
      branch.eval(this);
      callNr = 0;
      args.clear();
      context.branch = null;
    }
  }

  private void log(final AbraBaseSite site)
  {
    if (trace)
    {
      Qupla.log("" + site);
      Qupla.log("" + stack[site.index]);
    }
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    final TritVector[] oldStack = stack;
    stack = new TritVector[branch.totalSites()];

    if (!evalBranchInputsMatch(branch))
    {
      value = null;
      for (final TritVector arg : args)
      {
        value = TritVector.concat(value, arg);
      }

      if (branch.specialType == AbraBaseBlock.TYPE_SLICE)
      {
        stack = oldStack;
        return;
      }

      for (final AbraSiteParam input : branch.inputs)
      {
        input.eval(this);
      }
    }

    for (final AbraSiteParam input : branch.inputs)
    {
      log(input);
    }

    for (final AbraSiteLatch latch : branch.latches)
    {
      latch.eval(this);
      log(latch);
    }

    for (final AbraSiteKnot site : branch.sites)
    {
      site.eval(this);
      log(site);
    }

    TritVector result = null;
    for (final AbraBaseSite output : branch.outputs)
    {
      result = TritVector.concat(result, stack[output.index]);
    }

    // save latch values for future use
    for (final AbraSiteLatch latch : branch.latches)
    {
      saveLatch(latch);
    }

    stack = oldStack;
    value = result;
  }

  private boolean evalBranchInputsMatch(final AbraBlockBranch branch)
  {
    if (args.size() != branch.inputs.size())
    {
      return false;
    }

    for (int i = 0; i < args.size(); i++)
    {
      final TritVector arg = args.get(i);
      final AbraBaseSite input = branch.inputs.get(i);
      if (arg.size() != input.size)
      {
        return false;
      }

      stack[input.index] = arg;
    }

    return true;
  }

  public TritVector evalEntity(final FuncEntity entity, final TritVector vector)
  {
    // avoid converting vector to string, which is slow
    Qupla.log("effect " + entity.func.params.get(0).typeInfo.toString(vector) + " : " + entity.func);

    args.clear();
    args.add(vector);

    callTrail[callNr++] = (byte) entity.block.index;
    callTrail[callNr++] = (byte) (entity.block.index >> 8);
    entity.block.eval(this);

    // avoid converting vector to string, which is slow
    Qupla.log("     return " + entity.func.returnExpr.typeInfo.toString(value) + " : " + entity.func.returnExpr);

    callNr = 0;
    return value;
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    args.clear();
    boolean isAllNull = true;
    for (final AbraBaseSite input : knot.inputs)
    {
      final TritVector arg = stack[input.index];
      isAllNull = isAllNull && arg.isNull();
      args.add(arg);
    }

    if (isAllNull)
    {
      stack[knot.index] = new TritVector(knot.size, '@');
      return;
    }

    if (callNr == 4000)
    {
      error("Exceeded function call nesting limit");
    }

    callTrail[callNr++] = (byte) knot.index;

    evalKnotSpecial(knot);

    stack[knot.index] = value;

    callNr--;
  }

  private void evalKnotSpecial(final AbraSiteKnot knot)
  {
    switch (knot.block.specialType)
    {
    case AbraBaseBlock.TYPE_CONSTANT:
      value = knot.block.constantValue;
      return;

    case AbraBaseBlock.TYPE_MERGE:
      evalMerge(knot);
      return;

    case AbraBaseBlock.TYPE_NULLIFY_FALSE:
      evalNullify(knot, TritConverter.BOOL_FALSE);
      return;

    case AbraBaseBlock.TYPE_NULLIFY_TRUE:
      evalNullify(knot, TritConverter.BOOL_TRUE);
      return;

    case AbraBaseBlock.TYPE_SLICE:
      evalSlice(knot);
      return;
    }

    if (knot.block.name != null)
    {
      if (usePrint && knot.block.name.startsWith("print_"))
      {
        value = args.get(0);
        Qupla.log(value.toString());
        return;
      }

      if (useBreak && knot.block.name.startsWith("break_"))
      {
        value = args.get(0);
        Qupla.log(value.toString());
        return;
      }
    }

    knot.block.eval(this);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    if (latch.references == 0)
    {
      return;
    }

    callTrail[callNr] = (byte) latch.index;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;

    // if state was saved before set latch to that value otherwise set to zero
    final StateValue stateValue = stateValues.get(call);
    if (stateValue != null)
    {
      stack[latch.index] = stateValue.value;
      return;
    }

    stack[latch.index] = new TritVector(latch.size, '0');
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    if (args.size() == 0 || args.size() > 3)
    {
      error("LUT needs 1 - 3 inputs");
    }

    int index = 13;
    int power = 1;
    for (final TritVector arg : args)
    {
      if (arg.size() != 1)
      {
        error("LUT inputs need to be exactly 1 trit");
      }

      switch (arg.trit(0))
      {
      case '-':
        index -= power;
        break;

      case '0':
        break;

      case '1':
        index += power;
        break;

      default:
        value = tritNull;
        return;
      }

      power *= 3;
    }

    switch (lut.lookup.charAt(index))
    {
    case '0':
      value = tritZero;
      return;

    case '1':
      value = tritOne;
      return;

    case '-':
      value = tritMin;
      return;
    }

    value = tritNull;
  }

  private void evalMerge(final AbraSiteKnot knot)
  {
    for (final AbraBaseSite input : knot.inputs)
    {
      if (input.size != knot.size)
      {
        error("Invalid merge input size");
      }
    }

    value = null;
    for (final TritVector arg : args)
    {
      if (arg.isNull())
      {
        continue;
      }

      if (value == null)
      {
        value = arg;
        continue;
      }

      error("Multiple non-null merge values");
    }

    if (value == null)
    {
      value = new TritVector(knot.size, '@');
    }
  }

  private void evalNullify(final AbraSiteKnot knot, final char selector)
  {
    if (args.size() != 2)
    {
      error("Nullify needs 2 inputs");
    }

    final TritVector flag = args.get(0);
    if (flag.size() != 1)
    {
      error("Nullify flag size is not 1");
    }

    final TritVector target = args.get(1);
    if (target.size() != knot.size)
    {
      error("Nullify target size mismatch");
    }

    final boolean select = flag.trit(0) == selector;
    value = select ? target : new TritVector(knot.size, '@');
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    stack[param.index] = value.slicePadded(param.offset, param.size);
  }

  private void evalSlice(final AbraSiteKnot knot)
  {
    value = null;
    for (final TritVector arg : args)
    {
      value = TritVector.concat(value, arg);
    }

    final AbraBlockBranch branch = (AbraBlockBranch) knot.block;
    value = value.slice(branch.offset, branch.size);
  }

  private void saveLatch(final AbraSiteLatch latch)
  {
    if (latch.references == 0 || latch.latchSite == null)
    {
      return;
    }

    value = stack[latch.latchSite.index];
    if (value.isNull())
    {
      // do not overwrite with null trits
      return;
    }

    callTrail[callNr] = (byte) latch.index;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;

    // state already saved?
    final StateValue stateValue = stateValues.get(call);
    if (stateValue != null)
    {
      // reset state?
      if (value.isZero())
      {
        stateValues.remove(call);
        return;
      }

      // overwrite entire state?
      if (value.isValue())
      {
        stateValue.value = value;
        return;
      }

      // merge values by skipping null trits
      final char[] buffer = new char[value.size()];
      for (int i = 0; i < value.size(); i++)
      {
        final char trit = value.trit(i);
        buffer[i] = trit == '@' ? stateValue.value.trit(i) : trit;
      }

      stateValue.value = new TritVector(new String(buffer));
      return;
    }

    // state was not saved yet

    // reset state?
    if (value.isZero())
    {
      // already reset
      return;
    }

    // save state, but replace nulls with zeroes
    call.path = Arrays.copyOf(callTrail, callNr + 1);
    call.value = value.isValue() ? value : new TritVector(value.trits().replace('@', '0'));
    stateValues.put(call, call);
  }
}
