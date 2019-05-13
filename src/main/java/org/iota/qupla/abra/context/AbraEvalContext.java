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
import org.iota.qupla.abra.block.site.AbraSiteMerge;
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

  private static final TritVector tritMin = new TritVector(1, '-');
  private static final TritVector tritNull = new TritVector(1, '@');
  private static final TritVector tritOne = new TritVector(1, '1');
  private static final TritVector tritZero = new TritVector(1, '0');
  private static final boolean useBreak = true;
  private static final boolean usePrint = true;

  public final ArrayList<TritVector> args = new ArrayList<>();
  public int callNr;
  public final byte[] callTrail = new byte[4096];
  public TritVector[] stack;
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
      final AbraSiteKnot knot = (AbraSiteKnot) branch.sites.remove(branch.sites.size() - 1);

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

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.specialType == AbraBaseBlock.TYPE_CONSTANT)
    {
      value = branch.constantValue;
      return;
    }

    if (branch.specialType == AbraBaseBlock.TYPE_NULLIFY_TRUE)
    {
      if (args.get(0).trit(0) != TritConverter.BOOL_TRUE)
      {
        value = branch.constantValue;
        return;
      }

      value = args.get(1);
      return;
    }

    if (branch.specialType == AbraBaseBlock.TYPE_NULLIFY_FALSE)
    {
      if (args.get(0).trit(0) != TritConverter.BOOL_FALSE)
      {
        value = branch.constantValue;
        return;
      }

      value = args.get(1);
      return;
    }

    if (branch.specialType == AbraBaseBlock.TYPE_SLICE)
    {
      if (args.size() == 1)
      {
        value = args.get(0).slice(branch.offset, branch.size);
        return;
      }
    }

    interceptCall(branch);

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

      for (final AbraBaseSite input : branch.inputs)
      {
        input.eval(this);
      }
    }

    // initialize latches with old values
    for (final AbraBaseSite latch : branch.latches)
    {
      initializeLatch(latch);
    }

    for (final AbraBaseSite site : branch.sites)
    {
      site.eval(this);
    }

    TritVector result = null;
    for (final AbraBaseSite output : branch.outputs)
    {
      output.eval(this);
      result = TritVector.concat(result, value);
    }

    // update latches with new values
    for (final AbraBaseSite latch : branch.latches)
    {
      updateLatch(latch);
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

    knot.block.eval(this);
    stack[knot.index] = value;

    callNr--;
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
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

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    value = null;
    for (final AbraBaseSite input : merge.inputs)
    {
      final TritVector mergeValue = stack[input.index];
      if (mergeValue.isNull())
      {
        continue;
      }

      if (value == null)
      {
        value = mergeValue;
        continue;
      }

      error("Multiple non-null merge values");
    }

    if (value == null)
    {
      value = new TritVector(merge.size, '@');
    }

    stack[merge.index] = value;
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    stack[param.index] = value.slicePadded(param.offset, param.size);
  }

  private void initializeLatch(final AbraBaseSite latch)
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

  private void interceptCall(final AbraBlockBranch branch)
  {
    if (branch.name != null)
    {
      if (usePrint && branch.name.startsWith("print_"))
      {
        Qupla.log(args.get(0).toString());
      }

      if (useBreak && branch.name.startsWith("break_"))
      {
        Qupla.log(args.get(0).toString());
      }
    }
  }

  private void updateLatch(final AbraBaseSite latch)
  {
    if (latch.references == 0)
    {
      return;
    }

    // evaluate latch but do not overwrite its stack value
    // in case another latch uses this one as well
    // it should always use the old value
    final TritVector oldValue = stack[latch.index];
    latch.eval(this);
    stack[latch.index] = oldValue;

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
