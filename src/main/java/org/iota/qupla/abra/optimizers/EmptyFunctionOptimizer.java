package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
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
  protected void processKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    // find and disable all function calls that do nothing

    // must be a function call that has a single input
    if (knot.inputs.size() != 1)
    {
      return;
    }

    if (block.sites.size() != 0 || block.latches.size() != 0)
    {
      // not an empty function
      return;
    }

    if (block.inputs.size() != 1 || block.outputs.size() != 1)
    {
      // not simply passing the input back to output
      return;
    }

    final AbraBaseSite knotInput = knot.inputs.get(0);

    final AbraSiteParam input = block.inputs.get(0);
    if (input.size != knotInput.size)
    {
      // some slicing going on
      return;
    }

    final AbraBaseSite output = block.outputs.get(0);
    if (output != input)
    {
      // WTF? how is this even possible?
      return;
    }

    if (knot.block.name != null)
    {
      if (knot.block.name.startsWith("print_") || knot.block.name.startsWith("break_"))
      {
        // keep dummy print/break functions, even though they are empty
        return;
      }
    }

    // well, looks like we have a candidate
    replaceSite(knot, knotInput);
    if (knotInput.name == null)
    {
      knotInput.name = knot.name;
    }
  }
}
