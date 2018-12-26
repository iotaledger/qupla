package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class SlicedInputOptimizer extends BaseOptimizer
{
  public SlicedInputOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    //TODO split up inputs that will be sliced later so that they
    //     are pre-sliced and add concat calls to replace them
  }
}
