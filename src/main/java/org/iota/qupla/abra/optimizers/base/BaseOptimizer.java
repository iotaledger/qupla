package org.iota.qupla.abra.optimizers.base;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class BaseOptimizer
{
  public AbraBlockBranch branch;
  public QuplaToAbraContext context;
  public int index;
  public boolean reverse;

  public BaseOptimizer(final QuplaToAbraContext context, final AbraBlockBranch branch)
  {
    this.context = context;
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

  protected void replaceSite(final AbraBaseSite site, final AbraBaseSite replacement)
  {
    if (site.hasNullifier())
    {
      // extra precaution not to lose info
      return;
    }

    replaceSite(site, replacement, branch.sites);
    replaceSite(site, replacement, branch.outputs);
    replaceSite(site, replacement, branch.latches);
  }

  private void replaceSite(final AbraBaseSite target, final AbraBaseSite replacement, final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite next : sites)
    {
      if (next instanceof AbraSiteMerge)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) next;
        for (int i = 0; i < merge.inputs.size(); i++)
        {
          if (merge.inputs.get(i) == target)
          {
            target.references--;
            replacement.references++;
            merge.inputs.set(i, replacement);
          }
        }
      }

      if (next.nullifyFalse == target)
      {
        target.references--;
        replacement.references++;
        next.nullifyFalse = replacement;
      }

      if (next.nullifyTrue == target)
      {
        target.references--;
        replacement.references++;
        next.nullifyTrue = replacement;
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
