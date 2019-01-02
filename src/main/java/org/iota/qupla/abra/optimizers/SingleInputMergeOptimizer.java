package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class SingleInputMergeOptimizer extends BaseOptimizer
{
  public SingleInputMergeOptimizer(final QuplaToAbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processMerge(final AbraSiteMerge merge)
  {
    if (merge.inputs.size() == 1)
    {
      // this leaves <merge> unreferenced
      replaceSite(merge, merge.inputs.get(0));
    }
  }
}
