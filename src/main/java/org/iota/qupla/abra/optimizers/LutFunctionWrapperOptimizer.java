package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.context.AbraContext;

public class LutFunctionWrapperOptimizer extends BaseOptimizer
{
  public LutFunctionWrapperOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    // replace lut wrapper function with direct lut operation

    // must be a function call with max 3 inputs
    if (!(knot.block instanceof AbraBlockBranch) || knot.inputs.size() > 3)
    {
      return;
    }

    // all input values must be single trit
    for (final AbraSite input : knot.inputs)
    {
      if (input.size != 1)
      {
        return;
      }
    }

    final AbraBlockBranch target = (AbraBlockBranch) knot.block;
    if (target.sites.size() != 0 || target.latches.size() != 0)
    {
      // not an otherwise empty function
      return;
    }

    if (target.inputs.size() != knot.inputs.size() || target.outputs.size() != 1)
    {
      // not simply passing the inputs to output LUT knot
      return;
    }

    // all input params must be single trit
    for (final AbraSite input : target.inputs)
    {
      if (input.size != 1)
      {
        return;
      }
    }

    if (target.outputs.get(0).getClass() != AbraSiteKnot.class)
    {
      // definitely not a lut lookup
      return;
    }

    final AbraSiteKnot output = (AbraSiteKnot) target.outputs.get(0);
    if (output.inputs.size() != 3 || output.size != 1 || !(output.block instanceof AbraBlockLut))
    {
      // not a lut lookup
      return;
    }

    // well, looks like we have a candidate
    // reroute knot directly to LUT
    final ArrayList<AbraSite> inputs = new ArrayList<>();
    for (final AbraSite input : output.inputs)
    {
      final int idx = target.inputs.indexOf(input);
      final AbraSite knotInput = knot.inputs.get(idx);
      inputs.add(knotInput);
      knotInput.references++;
    }

    for (final AbraSite input : knot.inputs)
    {
      input.references--;
    }

    knot.inputs = inputs;
    knot.block = output.block;
  }
}
