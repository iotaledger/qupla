package org.iota.qupla.abra.optimizers.base;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.UnreferencedSiteRemover;
import org.iota.qupla.exception.CodeException;

public class BaseOptimizer
{
  protected AbraBlockBranch branch;
  protected int index;
  protected AbraModule module;
  protected boolean reverse;

  protected BaseOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    this.module = module;
    this.branch = branch;
  }

  protected void error(final String text)
  {
    throw new CodeException(text);
  }

  protected void processInputs()
  {
  }

  protected void processKnot(final AbraSiteKnot knot)
  {
    if (knot.references == 0)
    {
      return;
    }

    if (knot.block instanceof AbraBlockSpecial)
    {
      processKnotSpecial(knot, (AbraBlockSpecial) knot.block);
      return;
    }

    if (knot.block instanceof AbraBlockBranch)
    {
      processKnotBranch(knot, (AbraBlockBranch) knot.block);
      return;
    }

    if (knot.block instanceof AbraBlockLut)
    {
      processKnotLut(knot, (AbraBlockLut) knot.block);
      return;
    }

    error("WTF?");
  }

  protected void processKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
  }

  protected void processKnotLut(final AbraSiteKnot knot, final AbraBlockLut lut)
  {
  }

  protected void processKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
  }

  protected void processKnots()
  {
    if (reverse)
    {
      for (index = branch.sites.size() - 1; index >= 0; index--)
      {
        final AbraSiteKnot site = branch.sites.get(index);
        processKnot(site);
      }

      return;
    }

    for (index = 0; index < branch.sites.size(); index++)
    {
      final AbraSiteKnot site = branch.sites.get(index);
      processKnot(site);
    }
  }

  protected void processOutputs()
  {
  }

  protected void replaceSite(final AbraBaseSite source, final AbraBaseSite target)
  {
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
    processInputs();
    processKnots();
    processOutputs();

    new UnreferencedSiteRemover(module, branch).run();
  }
}
