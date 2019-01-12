package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;

public class AbraOrderBlockContext extends AbraTritCodeBaseContext
{
  private final HashSet<AbraBlockBranch> input = new HashSet<>();
  private final ArrayList<AbraBlockBranch> output = new ArrayList<>();

  @Override
  public void eval(final AbraModule module)
  {
    input.addAll(module.branches);

    super.eval(module);

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
}
