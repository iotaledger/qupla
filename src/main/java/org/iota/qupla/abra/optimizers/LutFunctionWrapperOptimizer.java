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
    if (!knot.block.couldBeLutWrapper())
    {
      return;
    }

    final AbraBlockBranch target = (AbraBlockBranch) knot.block;
    if (target.sites.size() != 1 || target.latches.size() != 0)
    {
      // too much going on to be a LUT wrapper
      return;
    }

    if (target.inputs.size() != knot.inputs.size() || target.outputs.size() != 1)
    {
      // not simply passing the inputs to output LUT knot
      return;
    }

    final AbraSiteKnot site = target.sites.get(0);
    if (site != target.outputs.get(0))
    {
      // definitely not a lut lookup
      return;
    }

    if (!(site.block instanceof AbraBlockLut) || site.inputs.size() != target.inputs.size())
    {
      // not a lut lookup
      return;
    }

    // well, looks like we have a candidate
    // reroute knot directly to LUT
    final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
    for (final AbraBaseSite input : site.inputs)
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
    knot.block = site.block;
  }
}
