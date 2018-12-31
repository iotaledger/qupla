package org.iota.qupla.abra;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class TritCode
{
  public static final int[] sizes = new int[300];
  public char[] buffer = new char[32];
  public int bufferOffset;

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

  public TritCode putInt(final int value, final int size)
  {
    final String trits = TritConverter.fromLong(value);
    putTrits(trits);
    if (size != trits.length())
    {
      putTrits(new TritVector(size - trits.length(), '0').trits());
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
