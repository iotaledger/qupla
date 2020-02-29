package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class AbraReadTritCodeContext extends AbraTritCodeBaseContext
{
  private ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  private final ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
  private AbraSiteParam constInput;

  @Override
  public void eval(final AbraModule module)
  {
    final int version = getInt();
    check(module.version == version, "Module version should be " + module.version);

    blocks.addAll(module.specials);

    module.luts.clear();
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
    branchTritCode.buffer = getTrits(getInt());
    branchTritCode.blocks = blocks;
    branchTritCode.evalBranchBuffer(branch);
  }

  private void evalBranchBuffer(final AbraBlockBranch branch)
  {
    // zero inputs cannot occur, so we can use that as a special flag
    // to indicate that every input is a single trit input
    // the actual amount of inputs follows immediately
    final int inputs = getInt();
    final boolean singleInputTrits = inputs == 0;
    final int inputSites = singleInputTrits ? getInt() : inputs;

    final int latchSites = getInt();
    final int bodySites = getInt();

    // zero outputs cannot occur, so we can use that as a special flag
    // to indicate that they are a concatenation of the final sites
    // in the body sites list, which is frequently the case anyway
    // the actual amount of outputs follows immediately
    final int outputs = getInt();
    final boolean lastOutputSites = outputs == 0;
    final int outputSites = lastOutputSites ? getInt() : outputs;

    int offset = 0;
    for (int i = 0; i < inputSites; i++)
    {
      final AbraSiteParam input = new AbraSiteParam();
      input.size = singleInputTrits ? 1 : getInt();
      input.offset = offset;
      offset += input.size;
      branch.inputs.add(input);
      branchSites.add(input);
    }
    constInput = branch.inputs.get(0);

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

    final int totalSites = branch.totalSites();
    offset = totalSites - outputSites;
    for (int i = 0; i < outputSites; i++)
    {
      final int outputIndex = lastOutputSites ? offset + i : getIndex(totalSites);
      final AbraBaseSite output = branchSites.get(outputIndex);
      branch.outputs.add(output);
      output.references++;
      branch.size += output.size;
    }

    // for each latch find out which site output it should be updated with
    for (int i = 0; i < latchSites; i++)
    {
      final AbraSiteLatch latch = branch.latches.get(i);
      final int latchIndex = getIndex(totalSites);
      if (latchIndex != 0)
      {
        latch.latchSite = branchSites.get(latchIndex);
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

    super.evalKnot(knot);
  }

  @Override
  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    getInputSites(knot, getInt());
  }

  @Override
  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
    getInputSites(knot, block.inputs());
  }

  @Override
  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_CONST:
      final TritVector vector = getConst();
      knot.size = vector.size();
      knot.block = new AbraBlockSpecial(block.index, knot.size, vector);
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      getInputSites(knot, 2);
      knot.block = new AbraBlockSpecial(block.index);
      break;

    case AbraBlockSpecial.TYPE_SLICE:
      getInputSites(knot, getInt());
      final int start = getInt();
      knot.size = getInt();
      knot.block = new AbraBlockSpecial(block.index, knot.size, start);
      break;

    default:
      getInputSites(knot, getInt());
      knot.block = new AbraBlockSpecial(block.index);
      break;
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
    // convert 35 trits to 54-bit long value, which encodes 27 bct trits
    final byte[] trits = getTrits(35);
    long value = TritConverter.toLong(trits);
    lut.fromLong(value);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
  }

  private TritVector getConst()
  {
    int size = getInt();
    if (size == 0)
    {
      // all zero trits, get the actual length
      size = getInt();
      return new TritVector(size, TritVector.TRIT_ZERO);
    }

    if (size <= 5)
    {
      // just get the trits
      return new TritVector(getTrits(size));
    }

    // trailing zeroes were trimmed, get remaining trits and reconstruct by padding zeroes
    final int len = getInt();
    final TritVector remain = new TritVector(getTrits(len));
    final TritVector zeroes = new TritVector(size - len, TritVector.TRIT_ZERO);
    return TritVector.concat(remain, zeroes);
  }

  private void getInputSites(final AbraSiteKnot knot, final int inputSites)
  {
    for (int i = 0; i < inputSites; i++)
    {
      final int inputIndex = getIndex(branchSites.size());
      final AbraBaseSite input = branchSites.get(inputIndex);
      knot.inputs.add(input);
      input.references++;
    }
  }

  @Override
  public String toString()
  {
    return toStringRead();
  }
}
