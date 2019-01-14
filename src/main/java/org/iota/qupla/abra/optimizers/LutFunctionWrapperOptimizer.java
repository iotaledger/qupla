package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class LutFunctionWrapperOptimizer extends BaseOptimizer
{
  public LutFunctionWrapperOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
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
    for (final AbraBaseSite input : knot.inputs)
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
    for (final AbraBaseSite input : target.inputs)
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
    final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
    for (final AbraBaseSite input : output.inputs)
    {
      final int idx = target.inputs.indexOf(input);
      final AbraBaseSite knotInput = knot.inputs.get(idx);
      inputs.add(knotInput);
      knotInput.references++;
    }

    for (final AbraBaseSite input : knot.inputs)
    {
      input.references--;
    }

    knot.inputs = inputs;
    knot.block = output.block;
  }
}
