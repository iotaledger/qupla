package org.iota.qupla.abra.optimizers.base;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;

public class BaseOptimizer
{
  public AbraBlockBranch branch;
  public int index;
  public AbraModule module;
  public boolean reverse;

  protected BaseOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    this.module = module;
    this.branch = branch;
  }

  private void process()
  {
    final AbraBaseSite site = branch.sites.get(index);
    if (site.references == 0)
    {
      return;
    }

    if (site.getClass() == AbraSiteMerge.class)
    {
      processMerge((AbraSiteMerge) site);
    }

    if (site.getClass() == AbraSiteKnot.class)
    {
      processKnot((AbraSiteKnot) site);
    }

    processSite((AbraSiteMerge) site);
  }

  protected void processKnot(final AbraSiteKnot knot)
  {
  }

  protected void processMerge(final AbraSiteMerge merge)
  {
  }

  protected void processSite(final AbraSiteMerge site)
  {
  }

  protected void replaceSite(final AbraBaseSite source, final AbraBaseSite target)
  {
    if (source.hasNullifier())
    {
      // extra precaution not to lose info
      return;
    }

    replaceSite(branch.sites, source, target);
    replaceSite(branch.outputs, source, target);
    replaceSite(branch.latches, source, target);
  }

  private void replaceSite(final ArrayList<? extends AbraBaseSite> siteList, final AbraBaseSite source, final AbraBaseSite target)
  {
    for (final AbraBaseSite site : siteList)
    {
      if (site instanceof AbraSiteMerge)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) site;
        for (int i = 0; i < merge.inputs.size(); i++)
        {
          if (merge.inputs.get(i) == source)
          {
            source.references--;
            target.references++;
            merge.inputs.set(i, target);
          }
        }
      }

      if (site.nullifyFalse == source)
      {
        source.references--;
        target.references++;
        site.nullifyFalse = target;
      }

      if (site.nullifyTrue == source)
      {
        source.references--;
        target.references++;
        site.nullifyTrue = target;
      }
    }
  }

  public void run()
  {
    if (reverse)
    {
      for (index = branch.sites.size() - 1; index >= 0; index--)
      {
        process();
      }
      return;
    }

    for (index = 0; index < branch.sites.size(); index++)
    {
      process();
    }
  }
}
