package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockImport;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteLatch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.IntegerExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritVector;

public class AbraEvalContext extends AbraCodeContext
{
  private static final TritVector minTrit = new TritVector(1, '-');
  private static final TritVector nullTrit = new TritVector(1, '@');
  private static final TritVector oneTrit = new TritVector(1, '1');
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();
  private static final TritVector zeroTrit = new TritVector(1, '0');
  public AbraContext abra;
  public ArrayList<TritVector> args = new ArrayList<>();
  public int callNr;
  public byte[] callTrail = new byte[1024];
  public TritVector[] stack;
  public TritVector value;

  public void error(final String text)
  {
    throw new CodeException(null, text);
  }

  public void eval(final AbraContext context, final BaseExpr expr)
  {
    abra = context;
    if (expr instanceof FuncExpr)
    {
      final FuncExpr funcExpr = (FuncExpr) expr;
      for (final AbraBlockBranch branch : context.abra.branches)
      {
        if (branch.origin == funcExpr.func)
        {
          args.clear();
          for (final BaseExpr arg : funcExpr.args)
          {
            if (arg instanceof IntegerExpr)
            {
              args.add(((IntegerExpr) arg).vector);
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
    if (branch.type == AbraBlock.TYPE_CONSTANT)
    {
      value = branch.constantValue;
      return;
    }

    if (branch.type == AbraBlock.TYPE_NULLIFY_TRUE)
    {
      if (args.get(0).trit(0) != '1')
      {
        value = branch.constantValue;
        return;
      }

      value = args.get(1);
      return;
    }

    if (branch.type == AbraBlock.TYPE_NULLIFY_FALSE)
    {
      if (args.get(0).trit(0) != '-')
      {
        value = branch.constantValue;
        return;
      }

      value = args.get(1);
      return;
    }

    if (branch.type == AbraBlock.TYPE_SLICE)
    {
      if (args.size() == 1)
      {
        value = args.get(0).slice(branch.offset, branch.size);
        return;
      }

      int breakpoint = 0;
    }

    final TritVector[] oldStack = stack;
    stack = new TritVector[branch.siteNr];

    if (!evalBranchInputsMatch(branch))
    {
      value = null;
      for (final TritVector arg : args)
      {
        value = TritVector.concat(value, arg);
      }

      if (branch.type == AbraBlock.TYPE_SLICE)
      {
        stack = oldStack;
        return;
      }

      for (final AbraSite input : branch.inputs)
      {
        input.eval(this);
      }
    }

    // initialize latches with old values
    for (final AbraSite latch : branch.latches)
    {
      initializeLatch(latch);
    }

    for (final AbraSite site : branch.sites)
    {
      site.eval(this);
    }

    TritVector result = null;
    for (final AbraSite output : branch.outputs)
    {
      output.eval(this);
      result = TritVector.concat(result, value);
    }

    // update latches with new values
    for (final AbraSite latch : branch.latches)
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
      final AbraSite input = branch.inputs.get(i);
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
    for (final AbraSite input : knot.inputs)
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
        value = zeroTrit;
        return;

      case '1':
        value = oneTrit;
        return;

      case '-':
        value = minTrit;
        return;
      }
    }

    value = nullTrit;
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    value = null;
    for (final AbraSite input : merge.inputs)
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

  private void initializeLatch(final AbraSite latch)
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

  private void updateLatch(final AbraSite latch)
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
