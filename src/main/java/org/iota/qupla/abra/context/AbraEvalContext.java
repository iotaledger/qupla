package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.VectorExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraEvalContext extends AbraBaseContext
{
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();

  private static final TritVector tritMin = new TritVector(1, '-');
  private static final TritVector tritNull = new TritVector(1, '@');
  private static final TritVector tritOne = new TritVector(1, '1');
  private static final TritVector tritZero = new TritVector(1, '0');

  public QuplaToAbraContext abra;
  public ArrayList<TritVector> args = new ArrayList<>();
  public int callNr;
  public byte[] callTrail = new byte[4096];
  public TritVector[] stack;
  public TritVector value;

  public void eval(final QuplaToAbraContext context, final BaseExpr expr)
  {
    abra = context;
    if (expr instanceof FuncExpr)
    {
      final FuncExpr funcExpr = (FuncExpr) expr;
      for (final AbraBlockBranch branch : context.abraModule.branches)
      {
        if (branch.origin == funcExpr.func)
        {
          args.clear();
          for (final BaseExpr arg : funcExpr.args)
          {
            if (arg instanceof VectorExpr)
            {
              args.add(((VectorExpr) arg).vector);
              continue;
            }

            error("Expected constant value");
          }

          branch.eval(this);
        }
      }
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
      if (args.get(0).trit(0) != '1')
      {
        value = branch.constantValue;
        return;
      }

      value = args.get(1);
      return;
    }

    if (branch.specialType == AbraBaseBlock.TYPE_NULLIFY_FALSE)
    {
      if (args.get(0).trit(0) != '-')
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
    if (args.size() != 3)
    {
      error("LUT needs exactly 3 inputs");
    }

    char trits[] = new char[3];
    for (int i = 0; i < 3; i++)
    {
      final TritVector arg = args.get(i);
      if (arg.size() != 1)
      {
        error("LUT inputs need to be exactly 1 trit");
      }

      trits[i] = arg.trit(0);
    }

    final Integer index = indexFromTrits.get(new String(trits));
    if (index != null)
    {
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
    if (value.size() < param.offset + param.size)
    {
      error("Insufficient input trits: " + value);
    }

    stack[param.index] = value.slice(param.offset, param.size);
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

  private void updateLatch(final AbraBaseSite latch)
  {
    if (latch.references == 0)
    {
      return;
    }

    latch.eval(this);

    // do NOT update latch site on stack with new value
    // other latches may need the old value still
    // again, do NOT add: stack[latch.index] = value;

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
}
