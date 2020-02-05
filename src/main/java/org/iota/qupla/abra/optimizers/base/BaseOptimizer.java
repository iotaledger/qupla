package org.iota.qupla.abra.optimizers.base;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.exception.CodeException;

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

  protected void error(final String text)
  {
    throw new CodeException(text);
  }

  private void process()
  {
    final AbraSiteKnot site = branch.sites.get(index);
    if (site.references != 0)
    {
      processKnot(site);
    }
  }

  protected void processKnot(final AbraSiteKnot knot)
  {
  }

  protected void replaceSite(final AbraBaseSite source, final AbraBaseSite target)
  {
    if (source.hasNullifier())
    {
      // extra precaution not to lose info
      return;
    }

    for (int i = 0; i < branch.latches.size(); i++)
    {
      final AbraSiteLatch latch = branch.latches.get(i);
      if (latch.latchSite == source)
      {
        source.references--;
        target.references++;
        latch.latchSite = target;
      }
    }

    for (final AbraSiteKnot site : branch.sites)
    {
      for (int i = 0; i < site.inputs.size(); i++)
      {
        if (site.inputs.get(i) == source)
        {
          source.references--;
          target.references++;
          site.inputs.set(i, target);
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

    for (int i = 0; i < branch.outputs.size(); i++)
    {
      final AbraBaseSite output = branch.outputs.get(i);
      if (output == source)
      {
        source.references--;
        target.references++;
        branch.outputs.set(i, target);
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
