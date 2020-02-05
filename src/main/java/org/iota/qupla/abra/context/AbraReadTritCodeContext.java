package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;
import org.iota.qupla.helper.TritConverter;

public class AbraReadTritCodeContext extends AbraTritCodeBaseContext
{
  public ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  public final ArrayList<AbraBaseSite> branchSites = new ArrayList<>();

  private void check(final boolean condition, final String errorText)
  {
    if (!condition)
    {
      error(errorText);
    }
  }

  @Override
  public void eval(final AbraModule module)
  {
    final int version = getInt();
    check(module.version == version, "Module version should be " + module.version);

    blocks.addAll(module.luts);

    final int luts = getInt();
    for (int i = 0; i < luts; i++)
    {
      final AbraBlockLut lut = new AbraBlockLut();
      module.luts.add(lut);
      blocks.add(lut);
    }

    final int branches = getInt();
    for (int i = 0; i < branches; i++)
    {
      final AbraBlockBranch branch = new AbraBlockBranch();
      module.branches.add(branch);
      blocks.add(branch);
    }

    final int imports = getInt();
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
    final int latchSites = getInt();
    final int bodySites = getInt();
    final int outputSites = getInt();

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

    for (int i = 0; i < latchSites; i++)
    {
      final AbraSiteLatch latch = new AbraSiteLatch();
      branch.latches.add(latch);
      latch.eval(this);
      branchSites.add(latch);
    }

    for (int i = 0; i < bodySites; i++)
    {
      final AbraSiteKnot site = new AbraSiteKnot();
      branch.sites.add(site);
      site.eval(this);
      branchSites.add(site);
    }

    branch.numberSites();

    for (int i = 0; i < outputSites; i++)
    {
      final int outputSite = getInt();
      check(outputSite < branchSites.size(), "Invalid output site index");
      final AbraBaseSite output = branchSites.get(outputSite);
      branch.outputs.add(output);
      output.references++;
      branch.size += output.size;
    }

    for (int i = 0; i < latchSites; i++)
    {
      final AbraSiteLatch latch = branch.latches.get(i);
      final int latchSite = getInt();
      check(latchSite < branchSites.size(), "Invalid latch site index");
      if (latchSite != 0)
      {
        latch.latchSite = branchSites.get(latchSite);
        latch.latchSite.references++;
      }
    }

    branch.name = "branch" + branch.index;
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
    final int blockNr = getInt();
    check(blockNr < blocks.size(), "Invalid block number");
    knot.block = blocks.get(blockNr);

    final int inputSites = getInt();
    for (int i = 0; i < inputSites; i++)
    {
      final int inputSite = getInt();
      check(inputSite < branchSites.size(), "Invalid input site index");
      final AbraBaseSite input = branchSites.get(inputSite);
      knot.inputs.add(input);
      input.references++;
    }

    if (blockNr == AbraBaseBlock.TYPE_SLICE)
    {
      final int start = getInt();
      knot.size = getInt();
      knot.slice(start);
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    latch.size = getInt();
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    if (lut.index < AbraModule.SPECIAL_LUTS)
    {
      return;
    }

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
  public void evalParam(final AbraSiteParam param)
  {
    param.size = getInt();
  }

  @Override
  public String toString()
  {
    return toStringRead();
  }
}
