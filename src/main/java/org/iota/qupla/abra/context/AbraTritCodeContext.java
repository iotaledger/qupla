package org.iota.qupla.abra.context;

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

public class AbraTritCodeContext extends AbraTritCodeBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    module.numberBlocks();

    putInt(0); // version
    putInt(module.luts.size());
    evalBlocks(module.luts);
    putInt(module.branches.size());
    evalBlocks(module.branches);
    putInt(module.imports.size());
    evalBlocks(module.imports);
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    // we need a separate temporary buffer to gather everything
    // before we can add the accumulated length and data
    final AbraTritCodeContext branchTritCode = new AbraTritCodeContext();
    branchTritCode.evalBranchSites(branch);

    // now copy the temporary buffer length and contents
    putInt(branchTritCode.bufferOffset);
    putTrits(new String(branchTritCode.buffer, 0, branchTritCode.bufferOffset));
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    putTrits(imp.hash);
    putInt(imp.blocks.size());
    for (final AbraBaseBlock block : imp.blocks)
    {
      putInt(block.index);
    }
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    putTrit('-');
    putSiteInputs(knot);
    putInt(knot.block.index);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    //TODO convert 27 bct lookup 'trits' to 35 trits
    putTrits(lut.lookup);
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    putTrit('1');
    putSiteInputs(merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    putInt(param.size);
  }

  private void putSiteInputs(final AbraSiteMerge merge)
  {
    putInt(merge.inputs.size());
    for (final AbraBaseSite input : merge.inputs)
    {
      putInt(merge.refer(input.index));
    }
  }
}
