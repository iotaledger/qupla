package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class BaseOptimizer
{
  public AbraBlockBranch branch;
  public AbraContext context;
  public int index;
  public boolean reverse;

  public BaseOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    this.context = context;
    this.branch = branch;
  }

  private void process()
  {
    final AbraSite site = branch.sites.get(index);
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

  protected void replaceSite(final AbraSite site, final AbraSite replacement)
  {
    replaceSite(site, replacement, branch.sites);
    replaceSite(site, replacement, branch.outputs);
    replaceSite(site, replacement, branch.latches);
  }

  private void replaceSite(final AbraSite target, final AbraSite replacement, final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite next : sites)
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
