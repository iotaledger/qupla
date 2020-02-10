package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockSpecial;
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
      if (!(input instanceof AbraSiteKnot))
      {
        outputs.add(input);
        continue;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) input;
      if (knot.block.index != AbraBlockSpecial.TYPE_CONCAT)
      {
        outputs.add(input);
        continue;
      }

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

  @Override
  protected void processKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    //TODO this will cause AbraToVerilog to fail because
    //     verilog code expects exact number of parameters
    //    evalInputs(knot.inputs);
  }

  @Override
  public void run()
  {
    super.run();

    evalInputs(branch.outputs);
  }
}
