package org.iota.qupla.abra.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.iota.qupla.Qupla;
import org.iota.qupla.abra.FpgaClient;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.dispatcher.entity.FuncEntity;
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraEvalContext extends AbraBaseContext
{
  private static FpgaClient client;
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();
  private static final boolean trace = false;
  private static final TritVector tritMin = new TritVector(1, TritVector.TRIT_MIN);
  private static final TritVector tritNull = new TritVector(1, TritVector.TRIT_NULL);
  private static final TritVector tritOne = new TritVector(1, TritVector.TRIT_ONE);
  private static final TritVector tritZero = new TritVector(1, TritVector.TRIT_ZERO);
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
      args.add(new TritVector(1, TritVector.TRIT_ZERO));
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
    branch.count++;

    if (branch.fpga && evalBranchFpga(branch))
    {
      return;
    }

    final TritVector[] oldStack = stack;
    stack = new TritVector[branch.totalSites()];

    if (!inputArgsMoveToStack(branch))
    {
      inputArgsSliceToStack(branch);
    }

    for (final AbraSiteParam input : branch.inputs)
    {
      trace(input);
    }

    for (final AbraSiteLatch latch : branch.latches)
    {
      latch.eval(this);
      trace(latch);
    }

    for (final AbraSiteKnot site : branch.sites)
    {
      site.eval(this);
      trace(site);
    }

    TritVector result = null;
    for (final AbraBaseSite output : branch.outputs)
    {
      result = TritVector.concat(result, stack[output.index]);
    }

    // save latch values for next time
    for (final AbraSiteLatch latch : branch.latches)
    {
      saveLatch(latch);
    }

    stack = oldStack;
    value = result;
  }

  private boolean evalBranchFpga(final AbraBlockBranch branch)
  {
    if (client == null)
    {
      client = new FpgaClient();
      try
      {
        final byte[] data = Files.readAllBytes(Paths.get(branch.name + ".qbc"));
        if (client.process('c', data) == null)
        {
          branch.fpga = false;
          return false;
        }
      }
      catch (IOException e)
      {
        branch.fpga = false;
        return false;
      }
    }


    value = null;
    for (final TritVector arg : args)
    {
      value = TritVector.concat(value, arg);
    }

    final byte[] input = value.trits();
    for (int i = 0; i < input.length; i++)
    {
      input[i] = TritVector.tritToBits(input[i]);
    }

    // pass input value to fpga for processing
    final byte[] output = client.process('d', input);
    if (output == null)
    {
      branch.fpga = false;
      return false;
    }

    for (int i = 0; i < output.length; i++)
    {
      output[i] = TritVector.bitsToTrit(output[i]);
    }

    value = new TritVector(output);
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

    if (knot.inputs.size() != 0)
    {
      boolean isAllNull = true;
      for (final AbraBaseSite input : knot.inputs)
      {
        final TritVector arg = stack[input.index];
        isAllNull = isAllNull && arg.isNull();
        args.add(arg);
      }

      if (isAllNull)
      {
        stack[knot.index] = new TritVector(knot.size, TritVector.TRIT_NULL);
        return;
      }
    }

    if (callNr == 4000)
    {
      error("Exceeded function call nesting limit");
    }

    callTrail[callNr++] = (byte) knot.index;

    if (knot.block.name != null)
    {
      if (usePrint && knot.block.name.startsWith("print_"))
      {
        value = args.get(0);
        stack[knot.index] = value;
        Qupla.log(value.toString());
        return;
      }

      if (useBreak && knot.block.name.startsWith("break_"))
      {
        value = args.get(0);
        stack[knot.index] = value;
        Qupla.log(value.toString());
        return;
      }
    }

    knot.block.eval(this);

    stack[knot.index] = value;

    callNr--;
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

    stack[latch.index] = new TritVector(latch.size, TritVector.TRIT_ZERO);
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
      case TritVector.TRIT_MIN:
        index -= power;
        break;

      case TritVector.TRIT_ZERO:
        break;

      case TritVector.TRIT_ONE:
        index += power;
        break;

      default:
        value = tritNull;
        return;
      }

      power *= 3;
    }

    switch (lut.lookup(index))
    {
    case TritVector.TRIT_ZERO:
      value = tritZero;
      return;

    case TritVector.TRIT_ONE:
      value = tritOne;
      return;

    case TritVector.TRIT_MIN:
      value = tritMin;
      return;
    }

    value = tritNull;
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    stack[param.index] = value.slicePadded(param.offset, param.size);
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_CONCAT:
    case AbraBlockSpecial.TYPE_SLICE:
      evalSpecialSlice(block);
      break;

    case AbraBlockSpecial.TYPE_CONST:
      value = block.constantValue;
      break;

    case AbraBlockSpecial.TYPE_MERGE:
      evalSpecialMerge(block);
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
      evalSpecialNullify(block, TritVector.TRIT_FALSE);
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      evalSpecialNullify(block, TritVector.TRIT_TRUE);
      break;
    }
  }

  private void evalSpecialMerge(final AbraBlockSpecial block)
  {
    if (args.size() > 3)
    {
      error("Merge needs 1-3 inputs");
    }

    value = null;
    for (final TritVector arg : args)
    {
      if (arg.size() != block.size)
      {
        error("Merge input size mismatch");
      }

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
      // all null args, just pass first arg
      value = args.get(0);
    }
  }

  private void evalSpecialNullify(final AbraBlockSpecial block, final byte selector)
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
    if (target.size() != block.size)
    {
      error("Nullify target size mismatch");
    }

    final boolean select = flag.trit(0) == selector;
    if (select)
    {
      value = target;
      return;
    }

    if (block.constantValue == null)
    {
      block.constantValue = new TritVector(block.size, TritVector.TRIT_NULL);
    }

    value = block.constantValue;
  }

  private void evalSpecialSlice(final AbraBlockSpecial block)
  {
    value = null;
    for (final TritVector arg : args)
    {
      value = TritVector.concat(value, arg);
    }

    if (value.size() < block.offset + block.size)
    {
      error("Slice input size is too short");
    }

    value = value.slice(block.offset, block.size);
  }

  private boolean inputArgsMoveToStack(final AbraBlockBranch branch)
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

  private void inputArgsSliceToStack(final AbraBlockBranch branch)
  {
    // turn args into a single concatenated trit vector
    value = null;
    for (final TritVector arg : args)
    {
      value = TritVector.concat(value, arg);
    }

    // let inputs extract their correct arg fragments to the stack
    for (final AbraSiteParam input : branch.inputs)
    {
      input.eval(this);
    }
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
      final byte[] buffer = new byte[value.size()];
      for (int i = 0; i < value.size(); i++)
      {
        final byte trit = value.trit(i);
        buffer[i] = trit == TritVector.TRIT_NULL ? stateValue.value.trit(i) : trit;
      }

      stateValue.value = new TritVector(buffer);
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
    call.value = value;
    if (!value.isValue())
    {
      final byte[] trits = value.trits();
      for (int i = 0; i < trits.length; i++)
      {
        if (trits[i] == TritVector.TRIT_NULL)
        {
          trits[i] = TritVector.TRIT_ZERO;
        }
      }

      value = new TritVector(trits);
    }

    stateValues.put(call, call);
  }

  private void trace(final AbraBaseSite site)
  {
    if (trace)
    {
      String in = " ";
      if (site instanceof AbraSiteKnot)
      {
        final AbraSiteKnot knot = (AbraSiteKnot) site;
        boolean first = true;
        for (final AbraBaseSite input : knot.inputs)
        {
          in += first ? "(" : ", ";
          first = false;
          final byte trit = stack[input.index].trit(0);
          final byte bits = TritVector.tritToBits(trit);
          in += "@1-0".charAt(bits);
        }
        in += ")";
      }
      Qupla.log("" + site);
      Qupla.log(stack[site.index] + in);
    }
  }
}
