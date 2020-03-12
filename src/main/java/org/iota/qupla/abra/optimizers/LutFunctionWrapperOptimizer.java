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
  protected void processKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    // replace lut wrapper function with direct lut operation

    if (knot.size != 1)
    {
      // knot value must be single trit
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

    // max 3 1-trit inputs, 1-trit output?
    if (!block.couldBeLutWrapper())
    {
      return;
    }

    if (block.sites.size() != 1 || block.latches.size() != 0)
    {
      // too much going on to be a LUT wrapper
      return;
    }

    if (block.inputs.size() != knot.inputs.size() || block.outputs.size() != 1)
    {
      // not simply passing the inputs to output LUT knot
      return;
    }

    final AbraSiteKnot site = block.sites.get(0);
    if (site != block.outputs.get(0))
    {
      // definitely not a lut lookup
      return;
    }

    if (!(site.block instanceof AbraBlockLut) || site.inputs.size() != block.inputs.size())
    {
      // not a lut lookup
      return;
    }

    // well, looks like we have a candidate
    // reroute knot directly to LUT
    final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
    for (final AbraBaseSite input : site.inputs)
    {
      final int idx = block.inputs.indexOf(input);
      final AbraBaseSite knotInput = knot.inputs.get(idx);
      inputs.add(knotInput);
      knotInput.references++;
    }

    for (final AbraBaseSite input : knot.inputs)
    {
      input.references--;
    }

    knot.inputs = inputs;
    knot.block = site.block;
  }
}
