package org.iota.qupla.abra.context.base;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.helper.TritConverter;

public abstract class AbraTritCodeBaseContext extends AbraBaseContext
{
  // ASCII code pages, with best encoding for text
  private static final String[] codePage = {
      " ABCDEFGHIJKLMNOPQRSTUVWXYZ",
      ".abcdefghijklmnopqrstuvwxyz",
      "0123456789_$,=+-*/%&|^?:;@#",
      "[]{}()<>!~`'\"\\\r\n\t"
  };
  private static final String[] codePageId = {
      "0",
      "1",
      "-0",
      "-1"
  };

  public static final int[] sizes = new int[300];

  public char[] buffer = new char[32];
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
    case '0':
      return getChar(0);

    case '1':
      return getChar(1);
    }

    switch (getTrit())
    {
    case '0':
      return getChar(2);

    case '1':
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
      final char trit = getTrit();
      if (trit != '0')
      {
        index += trit == '-' ? -power : power;
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
    char trit = getTrit();
    if (trit == '-')
    {
      // shortcut, final bit found already
      // so value can only be zero
      return 0;
    }

    // build up the value bit by bit
    int value = 0;
    int mask = 1;
    for (; trit != '-'; trit = getTrit())
    {
      // '0' or '1' bit at this position
      if (trit == '1')
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
    if (getTrit() == '0')
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

  protected char getTrit()
  {
    check(bufferOffset < buffer.length, "Buffer overflow in getTrit");
    return buffer[bufferOffset++];
  }

  protected String getTrits(final int size)
  {
    bufferOffset += size;
    check(bufferOffset <= buffer.length, "Buffer overflow in getTrits");
    return new String(buffer, bufferOffset - size, size);
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

    return putTrits("--").putInt(c);
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
      putTrit((v & 1) == 1 ? '1' : '0');
    }

    return putTrit('-');
  }

  protected AbraTritCodeBaseContext putString(final String text)
  {
    if (text == null)
    {
      return putTrit('0');
    }

    putTrit('1');
    putInt(text.length());
    for (int i = 0; i < text.length(); i++)
    {
      putChar(text.charAt(i));
    }

    return this;
  }

  protected AbraTritCodeBaseContext putTrit(final char trit)
  {
    if (bufferOffset < buffer.length)
    {
      buffer[bufferOffset++] = trit;
      return this;
    }

    // expand buffer
    return putTrits("" + trit);
  }

  protected AbraTritCodeBaseContext putTrits(final String trits)
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
