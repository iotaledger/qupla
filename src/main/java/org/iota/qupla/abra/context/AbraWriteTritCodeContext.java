package org.iota.qupla.abra.context;

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
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class AbraWriteTritCodeContext extends AbraTritCodeBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    module.numberBlocks();

    putInt(module.version);

    putInt(module.luts.size());
    putInt(module.branches.size());
    putInt(module.imports.size());

    evalBlocks(module.luts);
    evalBlocks(module.branches);
    evalBlocks(module.imports);
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    // we need a separate temporary buffer to gather everything
    // before we can add the accumulated length and data
    final AbraWriteTritCodeContext branchTritCode = new AbraWriteTritCodeContext();
    branchTritCode.evalBranchSites(branch);

    // now copy the temporary buffer length and contents
    putInt(branchTritCode.bufferOffset);
    putTrits(new String(branchTritCode.buffer, 0, branchTritCode.bufferOffset));
  }

  @Override
  protected void evalBranchSites(final AbraBlockBranch branch)
  {
    putInt(branch.inputs.size());
    putInt(branch.sites.size());
    putInt(branch.outputs.size());
    putInt(branch.latches.size());

    super.evalBranchSites(branch);
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    //    putTrits(imp.hash);
    //    putInt(imp.blocks.size());
    //    for (final AbraBaseBlock block : imp.blocks)
    //    {
    //      putInt(block.index);
    //    }
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
    // encode 27 bct trits as 54-bit long value, and convert to 35 trits
    long value = 0;
    for (int i = 26; i >= 0; i--)
    {
      value <<= 2;
      switch (lut.lookup.charAt(i))
      {
      case '0':
        value += 1;
        break;
      case '1':
        value += 2;
        break;
      case '-':
        value += 3;
        break;
      case '@':
        break;
      }
    }

    final String trits = TritConverter.fromLong(value);
    putTrits(trits);
    if (trits.length() < 35)
    {
      putTrits(new TritVector(35 - trits.length(), '0').trits());
    }
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
      //TODO can refer relative to merge.index here
      // if (input.index < merge.index)
      // {
      //   putInt(merge.index - 1 - input.index);
      //   continue;
      // }

      putInt(input.index);
    }
  }
}
