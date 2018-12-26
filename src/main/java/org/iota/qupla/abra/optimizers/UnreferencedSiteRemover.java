package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.expression.base.BaseExpr;

public class UnreferencedSiteRemover extends BaseOptimizer
{
  public UnreferencedSiteRemover(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
    reverse = true;
  }

  private void moveSiteStmtToNextSite(final BaseExpr stmt)
  {
    if (stmt == null)
    {
      return;
    }

    if (index < branch.sites.size())
    {
      // attach statement to next body site
      final AbraSite nextSite = branch.sites.get(index);
      if (nextSite.stmt == null) //TODO
      {
        nextSite.stmt = stmt;
      }
      return;
    }

    // attach statement to first output site
    final AbraSite nextSite = branch.outputs.get(0);
    if (nextSite.stmt == null) //TODO
    {
      nextSite.stmt = stmt;
    }
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    if (site.references != 0)
    {
      return;
    }

    updateReferenceCounts(site);

    branch.sites.remove(index);

    moveSiteStmtToNextSite(site.stmt);
  }

  private void updateReferenceCounts(final AbraSiteMerge site)
  {
    for (final AbraSite input : site.inputs)
    {
      input.references--;
    }

    if (site.nullifyFalse != null)
    {
      site.nullifyFalse.references--;
    }

    if (site.nullifyTrue != null)
    {
      site.nullifyTrue.references--;
    }
  }
}
