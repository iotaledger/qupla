package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class ConcatenationOptimizer extends BaseOptimizer
{
  public ConcatenationOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    //TODO replace concatenation knot that is passed as input to a knot
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
