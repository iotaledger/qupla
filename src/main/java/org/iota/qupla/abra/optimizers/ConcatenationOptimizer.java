package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class ConcatenationOptimizer extends BaseOptimizer
{
  public ConcatenationOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private void evalInputs(final ArrayList<AbraBaseSite> inputs)
  {
    final ArrayList<AbraBaseSite> outputs = new ArrayList<>();

    for (final AbraBaseSite input : inputs)
    {
      if (!isConcat(input))
      {
        outputs.add(input);
        continue;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) input;
      knot.references--;
      for (final AbraBaseSite knotInput : knot.inputs)
      {
        outputs.add(knotInput);
        knotInput.references++;
      }
    }

    inputs.clear();
    inputs.addAll(outputs);
  }

  private boolean isConcat(final AbraBaseSite output)
  {
    if (!(output instanceof AbraSiteKnot))
    {
      return false;
    }

    final AbraSiteKnot knot = (AbraSiteKnot) output;
    if (knot.block.specialType != AbraBaseBlock.TYPE_SLICE)
    {
      return false;
    }

    final AbraBlockBranch branch = (AbraBlockBranch) knot.block;
    if (branch.offset != 0)
    {
      // slice operation
      return false;
    }

    int totalSize = 0;
    for (final AbraBaseSite input : knot.inputs)
    {
      totalSize += input.size;
    }

    // concat returns everything, if it does not it is a slice operation
    return totalSize == branch.size;
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    //TODO this will cause AbraToVerilog to fail because
    //     verilog code expects exact number of parameters

    //    if (knot.block.specialType != 0)
    //    {
    //      // do not replace concat parameters to special blocks
    //      return;
    //    }
    //
    //    evalInputs(knot.inputs);
  }

  @Override
  public void run()
  {
    super.run();

    evalInputs(branch.outputs);
  }
}
