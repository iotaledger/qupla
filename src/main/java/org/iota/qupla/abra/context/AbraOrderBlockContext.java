package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;

public class AbraOrderBlockContext extends AbraTritCodeBaseContext implements Comparator<AbraBlockBranch>
{
  private final TreeSet<AbraBlockBranch> input = new TreeSet<>(this);
  private final ArrayList<AbraBlockBranch> output = new ArrayList<>();

  @Override
  public int compare(final AbraBlockBranch lhs, final AbraBlockBranch rhs)
  {
    if (lhs.size != rhs.size)
    {
      return lhs.size < rhs.size ? -1 : 1;
    }

    if (lhs.offset != rhs.offset)
    {
      return lhs.offset < rhs.offset ? -1 : 1;
    }

    if (lhs.constantValue != null && rhs.constantValue != null)
    {
      return lhs.constantValue.trits().compareTo(rhs.constantValue.trits());
    }

    if (lhs.index != rhs.index)
    {
      return lhs.index < rhs.index ? -1 : 1;
    }

    return 0;
  }

  @Override
  public void eval(final AbraModule module)
  {
    module.numberBlocks();

    evalConstant(module, true);
    evalConstant(module, false);
    evalSpecial(module, AbraBaseBlock.TYPE_NULLIFY_TRUE);
    evalSpecial(module, AbraBaseBlock.TYPE_NULLIFY_FALSE);
    evalSpecial(module, AbraBaseBlock.TYPE_MERGE);
    evalSpecial(module, AbraBaseBlock.TYPE_SLICE);

    for (final AbraBlockBranch branch : module.branches)
    {
      if (branch.specialType == 0)
      {
        input.add(branch);
      }
    }

    super.eval(module);

    input.clear();

    module.branches = output;

    module.blocks.clear();
    module.blocks.addAll(module.luts);
    module.blocks.addAll(module.branches);
    module.blocks.addAll(module.imports);

    module.numberBlocks();
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (!input.contains(branch))
    {
      return;
    }

    input.remove(branch);

    for (final AbraBaseSite site : branch.sites)
    {
      site.eval(this);
    }

    for (final AbraBaseSite output : branch.outputs)
    {
      output.eval(this);
    }

    for (final AbraBaseSite latch : branch.latches)
    {
      latch.eval(this);
    }

    output.add(branch);
  }

  private void evalConstant(final AbraModule module, final boolean isZero)
  {
    for (final AbraBlockBranch branch : module.branches)
    {
      if (branch.specialType == AbraBaseBlock.TYPE_CONSTANT && branch.constantValue.isZero() == isZero)
      {
        input.add(branch);
      }
    }

    output.addAll(input);

    input.clear();
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    if (knot.block instanceof AbraBlockBranch)
    {
      evalBranch((AbraBlockBranch) knot.block);
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
  }

  private void evalSpecial(final AbraModule module, final int specialType)
  {
    for (final AbraBlockBranch branch : module.branches)
    {
      if (branch.specialType == specialType)
      {
        input.add(branch);
      }
    }

    output.addAll(input);

    input.clear();
  }
}
