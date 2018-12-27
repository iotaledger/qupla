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

  private boolean moveSiteStmtToNextSite(final BaseExpr stmt)
  {
    if (stmt == null)
    {
      return true;
    }

    if (index + 1 < branch.sites.size())
    {
      // attach statement to next body site
      final AbraSite nextSite = branch.sites.get(index + 1);
      if (nextSite.stmt == null) //TODO
      {
        nextSite.stmt = stmt;
        return true;
      }

      return false;
    }

    // attach statement to first output site
    final AbraSite nextSite = branch.outputs.get(0);
    if (nextSite.stmt == null) //TODO
    {
      nextSite.stmt = stmt;
      return true;
    }

    return false;
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    if (site.references != 0 || site.hasNullifier())
    {
      return;
    }

    updateReferenceCounts(site);

    if (moveSiteStmtToNextSite(site.stmt))
    {
      branch.sites.remove(index);
    }
  }

  @Override
  public void run()
  {
    for (index = branch.sites.size() - 1; index >= 0; index--)
    {
      processSite((AbraSiteMerge) branch.sites.get(index));
    }
  }

  private void updateReferenceCounts(final AbraSiteMerge site)
  {
    for (final AbraSite input : site.inputs)
    {
      input.references--;
    }

    site.inputs.clear();

    if (site.nullifyFalse != null)
    {
      site.nullifyFalse.references--;
      site.nullifyFalse = null;
    }

    if (site.nullifyTrue != null)
    {
      site.nullifyTrue.references--;
      site.nullifyTrue = null;
    }
  }
}
