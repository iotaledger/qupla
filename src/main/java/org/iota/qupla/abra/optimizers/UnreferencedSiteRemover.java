package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class UnreferencedSiteRemover extends BaseOptimizer
{
  public UnreferencedSiteRemover(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
    reverse = true;
  }

  private void moveSiteStmtToNextSite(final BaseExpr stmt)
  {
    if (stmt == null)
    {
      return;
    }

    // find last statement in chain
    BaseExpr last = stmt;
    while (last.next != null)
    {
      last = last.next;
    }

    if (index + 1 < branch.sites.size())
    {
      // link statement(s) to next body site
      final AbraBaseSite site = branch.sites.get(index + 1);
      last.next = site.stmt;
      site.stmt = stmt;
      return;
    }

    // link statement(s) to outputs
    last.next = branch.finalStmt;
    branch.finalStmt = stmt;
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    if (knot.references != 0 || knot.hasNullifier())
    {
      return;
    }

    updateReferenceCounts(knot);

    moveSiteStmtToNextSite(knot.stmt);
    branch.sites.remove(index);
  }

  private void updateReferenceCounts(final AbraSiteKnot site)
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
