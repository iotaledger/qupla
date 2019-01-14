package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class SingleInputMergeOptimizer extends BaseOptimizer
{
  public SingleInputMergeOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
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
