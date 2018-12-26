package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class NullifyInserter extends BaseOptimizer
{
  public NullifyInserter(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  private void insertNullify(final AbraSite condition, final boolean trueFalse)
  {
    final AbraSite site = branch.sites.get(index);

    // create a site for nullify<site.size>(conditon, site)
    final AbraSiteKnot nullify = new AbraSiteKnot();
    nullify.size = site.size;
    nullify.inputs.add(condition);
    condition.references++;
    nullify.inputs.add(site);
    site.references++;
    nullify.nullify(context, trueFalse);

    replaceSite(site, nullify);

    branch.sites.add(index + 1, nullify);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    if (site.nullifyFalse != null)
    {
      insertNullify(site.nullifyFalse, false);
      return;
    }

    if (site.nullifyTrue != null)
    {
      insertNullify(site.nullifyTrue, true);
    }
  }
}
