package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.Qupla;
import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
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

  private void clearSizes(final ArrayList<AbraBlockBranch> branches)
  {
    for (final AbraBlockBranch branch : branches)
    {
      branch.size = 0;
      for (final AbraSiteKnot site : branch.sites)
      {
        site.size = 0;
      }
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
    module.numberBlocks();

    // we're going to recalculate all branch and body site sizes
    clearSizes(module.branches);

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
        check(knot.size != 0 || knot.references == 0);
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
    if (branch.analyzed)
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

    branch.analyzed = lastMissing == missing;
  }

  private int evalBranchSites(int index, final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
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
    if (knot.block.size() == 0)
    {
      knot.block.eval(this);
    }

    super.evalKnot(knot);

    if (knot.size == 0)
    {
      missing++;
    }
  }

  @Override
  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    check(knot.inputs.size() > 0);
    knot.size = block.size;

    //TODO analyze more?
  }

  @Override
  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
    check(knot.inputs.size() > 0 && knot.inputs.size() <= 3);

    for (final AbraBaseSite input : knot.inputs)
    {
      // either we cannot determine size or else it is 1
      ensure(input.size <= 1);
    }

    knot.size = 1;
  }

  @Override
  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_CONCAT:
      check(knot.inputs.size() > 1);
      int size = 0;
      for (final AbraBaseSite input : knot.inputs)
      {
        if (input.size == 0)
        {
          return;
        }

        size += input.size;
      }
      knot.size = size;
      break;

    case AbraBlockSpecial.TYPE_CONST:
      check(knot.inputs.size() == 0);
      knot.size = block.size;
      break;

    case AbraBlockSpecial.TYPE_SLICE:
      check(knot.inputs.size() > 0);
      knot.size = block.size;
      break;

    case AbraBlockSpecial.TYPE_MERGE:
      check(knot.inputs.size() == 2 || knot.inputs.size() == 1);

      size = 0;
      for (final AbraBaseSite input : knot.inputs)
      {
        // all equal sizes, so first non-zero one will do
        if (size == 0 && input.size != 0)
        {
          size = input.size;
        }

        // either not defined yet or same as other sizes
        ensure(input.size == 0 || input.size == size);
      }

      knot.size = size;
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      check(knot.inputs.size() == 2);
      final AbraBaseSite flag = knot.inputs.get(0);
      final AbraBaseSite value = knot.inputs.get(1);
      ensure(flag.size <= 1);
      knot.size = value.size;
      break;
    }

    if (block.size == 0)
    {
      knot.block = new AbraBlockSpecial(block.index, knot.size);
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    lut.analyzed = true;
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    check(param.offset == offset);

    ensure(param.size > 0);

    param.offset = offset;
    offset += param.size;
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
    block.analyzed = true;
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

      new AbraPrintContext("AbraAnalyzed.txt").eval(module);

      error("Recursion issue detected");
    }
  }
}
