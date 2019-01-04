package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class UnreferencedSiteRemover extends BaseOptimizer
{
  public UnreferencedSiteRemover(final QuplaToAbraContext context, final AbraBlockBranch branch)
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

    // link statement(s) to next body site or to first output site
    final boolean useBody = index + 1 < branch.sites.size();
    final AbraBaseSite site = useBody ? branch.sites.get(index + 1) : branch.outputs.get(0);
    BaseExpr last = stmt;
    while (last.next != null)
    {
      last = last.next;
    }

    last.next = site.stmt;
    site.stmt = stmt;
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    if (site.references != 0 || site.hasNullifier())
    {
      return;
    }

    updateReferenceCounts(site);

    moveSiteStmtToNextSite(site.stmt);
    branch.sites.remove(index);
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
    for (final AbraBaseSite input : site.inputs)
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
