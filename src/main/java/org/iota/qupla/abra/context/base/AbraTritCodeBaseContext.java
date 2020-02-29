package org.iota.qupla.abra.context.base;

import java.util.ArrayList;
import java.util.Arrays;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public abstract class AbraTritCodeBaseContext extends AbraBaseContext
{
  // ASCII code pages, with best encoding for text
  private static final String[] codePage = {
      " ABCDEFGHIJKLMNOPQRSTUVWXYZ",
      ".abcdefghijklmnopqrstuvwxyz",
      "0123456789_$,=+-*/%&|^?:;@#",
      "[]{}()<>!~`'\"\\\r\n\t"
  };
  private static final byte[][] codePageId = {
      new TritVector("0").trits(),
      new TritVector("1").trits(),
      new TritVector("-0").trits(),
      new TritVector("-1").trits()
  };

  public static final int[] sizes = new int[300];

  public byte[] buffer = new byte[32];
  public int bufferOffset;

  protected void check(final boolean condition, final String errorText)
  {
    if (!condition)
    {
      error(errorText);
    }
  }

  protected void evalBranchSites(final AbraBlockBranch branch)
  {
    // make sure sites are numbered correctly+
    branch.numberSites();

    evalSites(branch.inputs);
    evalSites(branch.latches);
    evalSites(branch.sites);
  }

  protected void evalSites(final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.eval(this);
    }
  }

  protected char getChar()
  {
    switch (getTrit())
    {
    case TritVector.TRIT_ZERO:
      return getChar(0);

    case TritVector.TRIT_ONE:
      return getChar(1);
    }

    switch (getTrit())
    {
    case TritVector.TRIT_ZERO:
      return getChar(2);

    case TritVector.TRIT_ONE:
      return getChar(3);
    }

    return (char) getInt();
  }

  private char getChar(final int codePageNr)
  {
    int index = 13;
    int power = 1;
    for (int i = 0; i < 3; i++)
    {
      final byte trit = getTrit();
      if (trit != TritVector.TRIT_ZERO)
      {
        index += trit == TritVector.TRIT_MIN ? -power : power;
      }

      power *= 3;
    }

    return codePage[codePageNr].charAt(index);
  }

  protected int getIndex(final int sites)
  {
    // see putIndex() for encoding algorithm
    final int index = sites - 1 - getInt();
    check(index < sites, "Invalid site index");
    return index;
  }

  protected int getInt()
  {
    // see putInt() for encoding algorithm
    byte trit = getTrit();
    if (trit == TritVector.TRIT_MIN)
    {
      // shortcut, final bit found already
      // so value can only be zero
      return 0;
    }

    // build up the value bit by bit
    int value = 0;
    int mask = 1;
    for (; trit != TritVector.TRIT_MIN; trit = getTrit())
    {
      // '0' or '1' bit at this position
      if (trit == TritVector.TRIT_ONE)
      {
        value |= mask;
      }

      mask <<= 1;
    }

    // final '1' bit found, encoded as -
    // add the '1' bit at this position
    value |= mask;

    // remember that we encoded the incremented value
    // so we have to decrement to get the actual value
    return value - 1;
  }

  protected String getString()
  {
    if (getTrit() == TritVector.TRIT_ZERO)
    {
      return null;
    }

    final int length = getInt();
    final char[] buffer = new char[length];
    for (int i = 0; i < length; i++)
    {
      buffer[i] = getChar();
    }

    return new String(buffer);
  }

  protected byte getTrit()
  {
    check(bufferOffset < buffer.length, "Buffer overflow in getTrit");
    return buffer[bufferOffset++];
  }

  protected byte[] getTrits(final int size)
  {
    bufferOffset += size;
    check(bufferOffset <= buffer.length, "Buffer overflow in getTrits");
    return Arrays.copyOfRange(buffer, bufferOffset - size, bufferOffset);
  }

  protected AbraTritCodeBaseContext putChar(final char c)
  {
    // encode ASCII as efficient as possible
    for (int page = 0; page < codePage.length; page++)
    {
      int index = codePage[page].indexOf(c);
      if (index >= 0)
      {
        return putTrits(codePageId[page]).putTrits(TritConverter.TRYTE_TRITS[index]);
      }
    }

    return putTrit(TritVector.TRIT_MIN).putTrit(TritVector.TRIT_MIN).putInt(c);
  }

  protected void putIndex(final int sites, final int index)
  {
    // encode as a relative index to <sites>, which is the amount
    // of sites that precedes the current encoding position
    // previous site becomes 0, the one before becomes 1, etc.
    // since sites often refer a previous one nearby the encoding location
    // this favors lower values that can be encoded in less trits
    putInt(sites - 1 - index);
  }

  protected AbraTritCodeBaseContext putInt(final int value)
  {
    sizes[value < 0 ? 299 : value < 298 ? value : 298]++;

    // The encoding is as follows: we expect only positive integers
    // Then we make sure there is at least a single '1' bit in the value
    // We achieve this by incrementing the value so that 0..n becomes 1..n+1
    // Now we encode starting with least significant bit every bit as 0 or 1,
    // and the final '1' bit will be encoded as -
    // All the rest of the bits can now be assumed to be zero
    for (int v = value + 1; v > 1; v >>= 1)
    {
      putTrit((v & 1) == 1 ? TritVector.TRIT_ONE : TritVector.TRIT_ZERO);
    }

    return putTrit(TritVector.TRIT_MIN);
  }

  protected AbraTritCodeBaseContext putString(final String text)
  {
    if (text == null)
    {
      return putTrit(TritVector.TRIT_ZERO);
    }

    putTrit(TritVector.TRIT_ONE);
    putInt(text.length());
    for (int i = 0; i < text.length(); i++)
    {
      putChar(text.charAt(i));
    }

    return this;
  }

  protected AbraTritCodeBaseContext putTrit(final byte trit)
  {
    if (bufferOffset < buffer.length)
    {
      buffer[bufferOffset++] = trit;
      return this;
    }

    // expand buffer
    return putTrits(new byte[] { trit });
  }

  protected AbraTritCodeBaseContext putTrits(final byte[] trits)
  {
    return putTrits(trits, trits.length);
  }

  protected AbraTritCodeBaseContext putTrits(final byte[] trits, final int length)
  {
    if (bufferOffset + length > buffer.length)
    {
      int bytes = buffer.length * 2;
      while (bufferOffset + length > bytes)
      {
        bytes *= 2;
      }

      // double buffer size and try again
      final byte[] old = buffer;
      buffer = new byte[bytes];
      System.arraycopy(old, 0, buffer, 0, bufferOffset);
    }

    System.arraycopy(trits, 0, buffer, bufferOffset, length);
    bufferOffset += length;
    return this;
  }

  protected String toStringRead()
  {
    // display 40 trit tail end of buffer
    final int remain = buffer.length - bufferOffset;
    final int len = remain < 40 ? remain : 40;
    return bufferOffset + " " + new String(buffer, bufferOffset, len);
  }

  protected String toStringWrite()
  {
    // display 40 trit tail end of buffer
    final int len = bufferOffset > 40 ? 40 : bufferOffset;
    return bufferOffset + " " + new String(buffer, bufferOffset - len, len);
  }
}
