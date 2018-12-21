package org.iota.qupla.abra;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.TritVector;

public class TritCode
{
  public char[] buffer = new char[0];
  public int bufferOffset;

  public int getInt(final int size)
  {
    final TritVector tmp = new TritVector();
    tmp.trits = getTrits(size);
    return (int) tmp.toLong();
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
      throw new CodeException(null, "Buffer overflow in getTrit");
    }

    return buffer[bufferOffset++];
  }

  public String getTrits(final int size)
  {
    bufferOffset += size;
    if (bufferOffset > buffer.length)
    {
      throw new CodeException(null, "Buffer overflow in getTrits(" + size + ")");
    }

    return new String(buffer, bufferOffset - size, size);
  }

  public TritCode putInt(final int value)
  {
    // sizes[value < 0 ? 299 : value < 298 ? value : 298]++;

    if (value >= 0)
    {
      if (value < 2)
      {
        return putTrit(value == 0 ? '0' : '1');
      }

      if (value == 2)
      {
        return putTrits("-0");
      }

      if (value < 30)
      {
        // 3-trit value
        return putTrits("-1").putInt(value - 3 - 13, 3);
      }
    }

    // encode as minimum required trits length/value
    final TritVector tmp = new TritVector();
    tmp.fromLong(value);
    return putTrits("--").putInt(tmp.trits.length()).putTrits(tmp.trits);
  }

  public TritCode putInt(final int value, final int size)
  {
    final TritVector tmp = new TritVector();
    tmp.fromLong(value);
    putTrits(tmp.trits);

    if (size != tmp.trits.length())
    {
      putTrits(TritVector.zero(size - tmp.trits.length()));
    }

    return this;
  }

  public TritCode putTrit(final char trit)
  {
    if (bufferOffset < buffer.length)
    {
      buffer[bufferOffset++] = trit;
      return this;
    }

    // expand buffer
    return putTrits("" + trit);
  }

  public TritCode putTrits(final String trits)
  {
    if (bufferOffset + trits.length() <= buffer.length)
    {
      final char[] copy = trits.toCharArray();
      System.arraycopy(copy, 0, buffer, bufferOffset, copy.length);
      bufferOffset += copy.length;
      return this;
    }

    // expand buffer by 10K chars and try again
    final char[] old = buffer;
    buffer = new char[old.length + 10000];
    System.arraycopy(old, 0, buffer, 0, bufferOffset);
    return putTrits(trits);
  }

  @Override
  public String toString()
  {
    final int charsLeft = buffer.length - bufferOffset;
    return new String(buffer, bufferOffset, charsLeft > 40 ? 40 : charsLeft);
  }
}
