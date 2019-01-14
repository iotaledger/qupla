package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class SlicedInputOptimizer extends BaseOptimizer
{
  public SlicedInputOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    //TODO split up inputs that will be sliced later so that they
    //     are pre-sliced and add concat calls to replace them
  }
}
