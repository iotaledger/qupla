package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class NullifyInserter extends BaseOptimizer
{
  public NullifyInserter(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private void insertNullify(final AbraBaseSite condition, final boolean trueFalse)
  {
    final AbraBaseSite site = branch.sites.get(index);

    // create a site for nullify<site.size>(conditon, site)
    final AbraSiteKnot nullify = new AbraSiteKnot();
    nullify.size = site.size;
    nullify.inputs.add(condition);
    nullify.nullify(module, trueFalse);

    site.nullifyFalse = null;
    site.nullifyTrue = null;
    branch.sites.add(index + 1, nullify);

    replaceSite(site, nullify);
    nullify.inputs.add(site);
    site.references++;
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
