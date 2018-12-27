package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class SingleInputMergeOptimizer extends BaseOptimizer
{
  public SingleInputMergeOptimizer(final AbraContext context, final AbraBlockBranch branch)
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
