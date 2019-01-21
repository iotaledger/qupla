package org.iota.qupla.abra.context;

import java.util.ArrayList;

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
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.TritConverter;

public class AbraReadTritCodeContext extends AbraTritCodeBaseContext
{
  public ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  public final ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
  private int totalSites;

  private void check(final boolean condition, final String errorText)
  {
    if (!condition)
    {
      throw new CodeException(errorText);
    }
  }

  @Override
  public void eval(final AbraModule module)
  {
    module.version = getInt();
    check(module.version == 0, "Module version should be 0");

    final int luts = getInt();
    final int branches = getInt();
    final int imports = getInt();

    for (int i = 0; i < luts; i++)
    {
      final AbraBlockLut lut = new AbraBlockLut();
      module.luts.add(lut);
      blocks.add(lut);
    }

    for (int i = 0; i < branches; i++)
    {
      final AbraBlockBranch branch = new AbraBlockBranch();
      module.branches.add(branch);
      blocks.add(branch);
    }

    for (int i = 0; i < imports; i++)
    {
      final AbraBlockImport imp = new AbraBlockImport();
      module.imports.add(imp);
      blocks.add(imp);
    }

    module.numberBlocks();

    evalBlocks(module.luts);
    evalBlocks(module.branches);
    evalBlocks(module.imports);
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    final AbraReadTritCodeContext branchTritCode = new AbraReadTritCodeContext();
    branchTritCode.buffer = getTrits(getInt()).toCharArray();
    branchTritCode.blocks = blocks;
    branchTritCode.evalBranchBuffer(branch);
  }

  private void evalBranchBuffer(final AbraBlockBranch branch)
  {
    final int inputSites = getInt();
    final int bodySites = getInt();
    final int outputSites = getInt();
    final int latchSites = getInt();

    totalSites = inputSites + bodySites + outputSites + latchSites;

    int offset = 0;
    for (int i = 0; i < inputSites; i++)
    {
      final AbraSiteParam input = new AbraSiteParam();
      branch.inputs.add(input);
      input.eval(this);
      input.offset = offset;
      offset += input.size;
      branchSites.add(input);
    }

    getBranchSites(bodySites, branch.sites);
    getBranchSites(outputSites, branch.outputs);
    getBranchSites(latchSites, branch.latches);

    branch.numberSites();

    rewireInputSites(branch.sites);
    rewireInputSites(branch.outputs);
    rewireInputSites(branch.latches);
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
    getSiteInputs(knot);

    final int blockNr = getInt();
    check(blockNr < blocks.size(), "Invalid block number");
    knot.block = blocks.get(blockNr);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    // convert 35 trits to 54-bit long value, which encodes 27 bct trits
    final String trits = getTrits(35);
    long value = TritConverter.toLong(trits);
    char buffer[] = new char[27];
    for (int i = 0; i < 27; i++)
    {
      buffer[i] = "@01-".charAt((int) value & 0x03);
      value >>= 2;
    }

    lut.lookup = new String(buffer);
    lut.name = AbraBlockLut.unnamed(lut.lookup);
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    getSiteInputs(merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    param.size = getInt();
  }

  private void getBranchSites(final int count, final ArrayList<AbraBaseSite> sites)
  {
    for (int i = 0; i < count; i++)
    {
      AbraBaseSite site = null;
      switch (getTrit())
      {
      case '-':
        site = new AbraSiteKnot();
        break;

      case '1':
        site = new AbraSiteMerge();
        break;

      default:
        check(false, "Expected site type trit");
      }

      sites.add(site);
      site.eval(this);
      branchSites.add(site);
    }
  }

  private void getSiteInputs(final AbraSiteMerge merge)
  {
    final int count = getInt();
    for (int i = 0; i < count; i++)
    {
      // use a placeholder that will hold the actual index
      // we will rewire this later to point to the actual site
      final AbraSiteParam input = new AbraSiteParam();
      input.index = getInt();
      check(input.index < totalSites, "Invalid site index");
      merge.inputs.add(input);
    }
  }

  private void rewireInputSites(final ArrayList<AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      final AbraSiteMerge merge = (AbraSiteMerge) site;
      for (int i = 0; i < merge.inputs.size(); i++)
      {
        final AbraBaseSite input = merge.inputs.get(i);
        //TODO can refer relative to merge.index here
        // if (input.index < merge.index)
        // {
        //   input.index = merge.index - 1 - input.index;
        // }

        final AbraBaseSite actualSite = branchSites.get(input.index);
        merge.inputs.set(i, actualSite);
        actualSite.references++;
      }
    }
  }
}
