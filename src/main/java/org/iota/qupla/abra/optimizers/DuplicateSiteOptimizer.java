package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class DuplicateSiteOptimizer extends BaseOptimizer
{
  public DuplicateSiteOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    for (int i = index + 1; i < branch.sites.size(); i++)
    {
      final AbraBaseSite next = branch.sites.get(i);
      if (next.isIdentical(knot))
      {
        replaceSite(next, knot);
      }
    }
  }
}
