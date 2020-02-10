package org.iota.qupla.abra.context;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
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
    boolean singleInputTrits = true;
    for (final AbraSiteParam input : branch.inputs)
    {
      if (input.size != 1)
      {
        singleInputTrits = false;
        break;
      }
    }

    // zero inputs cannot occur, so we can use that as a special flag
    // to indicate that every input is a single trit input
    // the actual amount of inputs follows immediately
    if (singleInputTrits)
    {
      putInt(0);
    }
    putInt(branch.inputs.size());

    putInt(branch.latches.size());
    putInt(branch.sites.size());

    boolean lastOutputSites = true;
    final int offset = branch.totalSites() - branch.outputs.size();
    for (int i = 0; i < branch.outputs.size(); i++)
    {
      final AbraBaseSite output = branch.outputs.get(i);
      if (output.index != offset + i)
      {
        lastOutputSites = false;
        break;
      }
    }

    // zero outputs cannot occur, so we can use that as a special flag
    // to indicate that they are a concatenation of the final sites
    // in the body sites list, which is frequently the case anyway
    // the actual amount of outputs follows immediately
    if (lastOutputSites)
    {
      putInt(0);
    }
    putInt(branch.outputs.size());

    // make sure sites are numbered correctly+
    branch.numberSites();

    // only specify input sizes when there are trit vectors > 1 trit
    if (!singleInputTrits)
    {
      evalSites(branch.inputs);
    }

    evalSites(branch.latches);
    evalSites(branch.sites);

    // only specify output sites when they are not the final body sites
    if (!lastOutputSites)
    {
      for (final AbraBaseSite output : branch.outputs)
      {
        putIndex(branch.totalSites(), output.index);
      }
    }

    // for each latch specify which site output it should be updated with
    for (final AbraSiteLatch latch : branch.latches)
    {
      final int latchIndex = latch.latchSite == null ? 0 : latch.latchSite.index;
      putIndex(branch.totalSites(), latchIndex);
    }
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
    putInt(knot.block.index);

    super.evalKnot(knot);
  }

  @Override
  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    putInt(knot.inputs.size());
    putInputSites(knot, knot.inputs.size());
  }

  @Override
  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
    putInputSites(knot, block.inputs());
  }

  @Override
  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_CONST:
      putConst(block.constantValue);
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      putInputSites(knot, 2);
      break;

    case AbraBlockSpecial.TYPE_SLICE:
      putInt(knot.inputs.size());
      putInputSites(knot, knot.inputs.size());
      putInt(block.offset);
      putInt(block.size);
      break;

    default:
      putInt(knot.inputs.size());
      putInputSites(knot, knot.inputs.size());
      break;
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    putInt(latch.size);
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
  public void evalParam(final AbraSiteParam param)
  {
    putInt(param.size);
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
  }

  private void putConst(final TritVector constant)
  {
    if (constant.isZero())
    {
      // all zero trits, encode length zero, followed by actual length
      putInt(0);
      putInt(constant.size());
      return;
    }

    // encode actual length
    putInt(constant.size());

    final String trits = constant.trits();
    if (constant.size() <= 5)
    {
      // just encode the trits, nothing to gain from compression
      putTrits(trits);
      return;
    }

    // when more than 5 trits, trim off all trailing zeroes
    // and encode the remaining length and trits
    // this optimization uses the fact that most constants are
    // small values (-1..2) and get zero-extended to much larger vectors

    // find final non-zero trit
    int len = trits.length();
    while (trits.charAt(len - 1) == '0')
    {
      len--;
    }

    // encode the remaining length and trits
    // we can reconstruct it because we already know the size
    putInt(len);
    putTrits(trits.substring(0, len));
  }

  private void putInputSites(final AbraSiteKnot knot, final int inputs)
  {
    for (int i = 0; i < inputs; i++)
    {
      final AbraBaseSite input = knot.inputs.get(i);
      putIndex(knot.index, input.index);
    }
  }

  @Override
  public String toString()
  {
    return toStringWrite();
  }
}
