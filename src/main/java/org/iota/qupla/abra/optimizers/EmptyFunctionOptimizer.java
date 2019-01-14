package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class EmptyFunctionOptimizer extends BaseOptimizer
{
  public EmptyFunctionOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    // find and disable all function calls that do nothing

    // must be a function call that has a single input
    if (!(knot.block instanceof AbraBlockBranch) || knot.inputs.size() != 1)
    {
      return;
    }

    final AbraBlockBranch target = (AbraBlockBranch) knot.block;
    if (target.sites.size() != 0 || target.latches.size() != 0)
    {
      // not an empty function
      return;
    }

    if (target.inputs.size() != 1 || target.outputs.size() != 1)
    {
      // not simply passing the input back to output
      return;
    }

    final AbraBaseSite knotInput = knot.inputs.get(0);

    final AbraSiteParam input = (AbraSiteParam) target.inputs.get(0);
    if (input.size != knotInput.size)
    {
      // some slicing going on
      return;
    }

    final AbraSiteMerge output = (AbraSiteMerge) target.outputs.get(0);
    if (output.getClass() != AbraSiteMerge.class || output.inputs.size() != 1)
    {
      // not a single-input merge
      return;
    }

    if (output.inputs.get(0) != input)
    {
      // WTF? how is this even possible?
      return;
    }

    if (output.size != knotInput.size)
    {
      // another WTF moment
      return;
    }

    // well, looks like we have a candidate
    replaceSite(knot, knotInput);
  }
}
