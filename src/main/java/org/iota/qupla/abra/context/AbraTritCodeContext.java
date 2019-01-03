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
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class AbraTritCodeContext extends AbraBaseContext
{
  public static final int[] sizes = new int[300];
  public char[] buffer = new char[32];
  public int bufferOffset;

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
    final AbraTritCodeContext branchTritCode = new AbraTritCodeContext();
    branchTritCode.evalBranchSites(branch);

    putInt(branchTritCode.bufferOffset);
    putTrits(new String(branchTritCode.buffer, 0, branchTritCode.bufferOffset));
  }

  public void evalBranchSites(final AbraBlockBranch branch)
  {
    branch.numberSites();

    putInt(branch.inputs.size());
    evalSites(branch.inputs);
    putInt(branch.sites.size());
    putInt(branch.outputs.size());
    putInt(branch.latches.size());
    evalSites(branch.sites);
    evalSites(branch.outputs);
    evalSites(branch.latches);
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

  public int getInt(final int size)
  {
    return TritConverter.toInt(getTrits(size));
  }

  public int getInt()
  {
    switch (getTrit())
    {
    case '0':
      return 0;

    case '1':
      return 1;
    }

    switch (getTrit())
    {
    case '0':
      return 2;

    case '1':
      // 3..29 : -13..13 (3 trits)
      return getInt(3) + 3 + 13;
    }

    // any other value is encoded as length-prefixed trit vector
    return getInt(getInt());
  }

  public char getTrit()
  {
    if (bufferOffset >= buffer.length)
    {
      throw new CodeException("Buffer overflow in getTrit");
    }

    return buffer[bufferOffset++];
  }

  public String getTrits(final int size)
  {
    bufferOffset += size;
    if (bufferOffset > buffer.length)
    {
      throw new CodeException("Buffer overflow in getTrits(" + size + ")");
    }

    return new String(buffer, bufferOffset - size, size);
  }

  public AbraTritCodeContext putInt(final int value)
  {
    sizes[value < 0 ? 299 : value < 298 ? value : 298]++;

    if (true)
    {
      // binary coded trit value
      int v = value;

      //      if (v != 0)
      //      {
      //        // slightly better encoding because powers of 2 are now encoded with 1 trit less
      //        v--;
      //        putTrit((v & 1) == 0 ? '-' : '1');
      //        for (v >>= 1; v != 0; v >>= 1)
      //        {
      //          putTrit((v & 1) == 0 ? '-' : '1');
      //        }
      //      }

      for (; v != 0; v >>= 1)
      {
        putTrit((v & 1) == 0 ? '-' : '1');
      }

      return putTrit('0');
    }

    // most-used trit value
    if (value < 2)
    {
      return putTrit(value == 0 ? '0' : '1');
    }

    putTrit('-');

    int v = value >> 1;
    if (v != 0)
    {
      v--;
      putTrit((v & 1) == 0 ? '-' : '1');
      for (v >>= 1; v != 0; v >>= 1)
      {
        putTrit((v & 1) == 0 ? '-' : '1');
      }
    }

    return putTrit('0');
  }

  public AbraTritCodeContext putInt(final int value, final int size)
  {
    final String trits = TritConverter.fromLong(value);
    putTrits(trits);
    if (size != trits.length())
    {
      putTrits(new TritVector(size - trits.length(), '0').trits());
    }

    return this;
  }

  public void putSiteInputs(final AbraSiteMerge merge)
  {
    putInt(merge.inputs.size());
    for (final AbraBaseSite input : merge.inputs)
    {
      putInt(merge.refer(input.index));
    }
  }

  public AbraTritCodeContext putTrit(final char trit)
  {
    if (bufferOffset < buffer.length)
    {
      buffer[bufferOffset++] = trit;
      return this;
    }

    // expand buffer
    return putTrits("" + trit);
  }

  public AbraTritCodeContext putTrits(final String trits)
  {
    if (bufferOffset + trits.length() <= buffer.length)
    {
      final char[] copy = trits.toCharArray();
      System.arraycopy(copy, 0, buffer, bufferOffset, copy.length);
      bufferOffset += copy.length;
      return this;
    }

    // double buffer size and try again
    final char[] old = buffer;
    buffer = new char[old.length * 2];
    System.arraycopy(old, 0, buffer, 0, bufferOffset);
    return putTrits(trits);
  }

  @Override
  public String toString()
  {
    final int start = bufferOffset > 40 ? bufferOffset - 40 : 0;
    return bufferOffset + " " + new String(buffer, start, bufferOffset - start);
  }
}
