package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class ConcatenatedOutputOptimizer extends BaseOptimizer
{
  public ConcatenatedOutputOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    //TODO replace concatenation by moving concatenated sites from body to outputs
  }

  @Override
  public void run()
  {
    for (index = 0; index < branch.outputs.size(); index++)
    {
      final AbraSite site = branch.outputs.get(index);
      processSite((AbraSiteMerge) site);
    }
  }
}
