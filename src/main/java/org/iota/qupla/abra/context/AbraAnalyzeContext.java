package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.Qupla;
import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraAnalyzeContext extends AbraBaseContext
{
  private static final boolean sanityCheck = true;
  private int missing;
  private int offset;

  private void check(final boolean condition)
  {
    if (sanityCheck && !condition)
    {
      error("Check failed");
    }
  }

  private void clearSizes(final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.size = 0;
    }
  }

  private void clearSizes(final AbraModule module)
  {
    for (final AbraBlockBranch branch : module.branches)
    {
      branch.size = 0;
      clearSizes(branch.sites);
    }
  }

  private void ensure(final boolean condition)
  {
    if (!condition)
    {
      error("Ensure failed");
    }
  }

  @Override
  public void eval(final AbraModule module)
  {
    // we're going to recalculate all sizes
    clearSizes(module);

    // single pass over everything
    super.eval(module);

    // some sizes may have been indeterminable for now due to recursion
    resolveRecursions(module);

    // quick sanity check if everything has a size now
    for (final AbraBlockBranch branch : module.branches)
    {
      check(branch.size != 0);
      for (final AbraSiteKnot knot : branch.sites)
      {
        check(knot.size != 0);
      }

      for (final AbraSiteLatch latch : branch.latches)
      {
        check(latch.latchSite == null || latch.latchSite.size == latch.size);
      }
    }
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.analyzed || //
        branch.specialType == AbraBaseBlock.TYPE_SLICE || //
        branch.specialType == AbraBaseBlock.TYPE_CONSTANT)
    {
      return;
    }

    branch.analyzed = true;

    offset = 0;
    int index = 0;
    int lastMissing = missing;
    index = evalBranchSites(index, branch.inputs);
    index = evalBranchSites(index, branch.latches);
    index = evalBranchSites(index, branch.sites);

    int size = 0;
    for (final AbraBaseSite output : branch.outputs)
    {
      if (output.size == 0)
      {
        // insufficient data to calculate return size yet
        branch.analyzed = false;
        return;
      }

      size += output.size;
    }

    branch.size = size;
    ensure(branch.size != 0);

    if (lastMissing != missing)
    {
      // come back later to fill in missing sites
      branch.analyzed = false;
      return;
    }

    branch.analyzed = true;
  }

  private int evalBranchSites(int index, final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      check(site.index == index);
      site.index = index++;

      site.eval(this);
    }

    return index;
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {

  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    check(knot.inputs.size() != 0 && knot.references != 0);

    knot.block.eval(this);
    if (knot.block.size() == 0)
    {
      missing++;
      return;
    }

    if (knot.block instanceof AbraBlockBranch)
    {
      knot.size = knot.block.size();
      evalKnotBranch(knot);
      return;
    }

    if (knot.block.specialType == AbraBaseBlock.TYPE_MERGE)
    {
      ensure(knot.inputs.size() <= 3);

      int size = 0;
      for (final AbraBaseSite input : knot.inputs)
      {
        if (size == 0 && input.size != 0)
        {
          size = input.size;
        }

        ensure(input.size == 0 || input.size == size);
      }

      if (size == 0)
      {
        missing++;
        return;
      }

      knot.size = size;
      return;
    }

    if (knot.block.specialType == AbraBaseBlock.TYPE_NULLIFY_TRUE || //
        knot.block.specialType == AbraBaseBlock.TYPE_NULLIFY_FALSE)
    {
      ensure(knot.inputs.size() == 2);

      final AbraBaseSite flag = knot.inputs.get(0);
      if (flag.size == 0)
      {
        missing++;
        return;
      }

      ensure(flag.size == 1);

      final AbraBaseSite value = knot.inputs.get(1);
      if (value.size == 0)
      {
        missing++;
        return;
      }

      knot.size = value.size;
      return;
    }

    // knot.block is a lut
    ensure(knot.inputs.size() <= 3);

    for (final AbraBaseSite input : knot.inputs)
    {
      if (input.size == 0)
      {
        missing++;
        return;
      }

      ensure(input.size == 1);
    }

    knot.size = 1;
  }

  private void evalKnotBranch(final AbraSiteKnot knot)
  {
    //TODO
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    if (lut.analyzed)
    {
      return;
    }

    lut.analyzed = true;

    switch (lut.index)
    {
    case 0:
      ensure(lut.specialType == AbraBaseBlock.TYPE_MERGE);
      break;

    case AbraBaseBlock.TYPE_NULLIFY_TRUE:
    case AbraBaseBlock.TYPE_NULLIFY_FALSE:
    case AbraBaseBlock.TYPE_SLICE:
      ensure(lut.specialType == lut.index);
      break;

    case AbraBaseBlock.TYPE_CONSTANT:
    case AbraBaseBlock.TYPE_CONSTANT + 1:
    case AbraBaseBlock.TYPE_CONSTANT + 2:
    case AbraBaseBlock.TYPE_CONSTANT + 3:
      ensure(lut.specialType == AbraBaseBlock.TYPE_CONSTANT);
      break;
    }
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    check(param.offset == offset);

    ensure(param.size > 0);
    ensure(param.nullifyFalse == null);
    ensure(param.nullifyTrue == null);

    param.offset = offset;
    offset += param.size;
  }

  private void resolveRecursions(final AbraModule module)
  {
    // did we encounter any missing branch sizes?
    int lastMissing = 0;
    while (missing != lastMissing)
    {
      // try to resolve missing ones by running another pass
      // over the ones that have not finished analyzing yet
      // and see if that results in less missing branch sizes
      lastMissing = missing;
      missing = 0;
      evalBlocks(module.branches);
    }

    if (missing != 0)
    {
      // still missing some branch sizes
      // must be due to recursion issues
      for (final AbraBlockBranch branch : module.branches)
      {
        if (branch.size() == 0)
        {
          Qupla.log("Unresolved trit vector size in branch: " + branch.name);
        }
      }

      error("Recursion issue detected");
    }
  }
}
