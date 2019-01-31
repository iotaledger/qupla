package org.iota.qupla.helper;

import org.iota.qupla.exception.CodeException;

public class TritVector
{
  private static final TritVectorBuffer nulls = new TritVectorBuffer(0);
  private static final TritVectorBuffer singleTrits = new TritVectorBuffer(2);
  private static final TritVectorBuffer zeroes = new TritVectorBuffer(0);

  public String name;
  private int offset;
  private int size;
  private int valueTrits;
  private TritVectorBuffer vector;

  public TritVector(final TritVector copy)
  {
    name = null;
    offset = copy.offset;
    size = copy.size;
    valueTrits = copy.valueTrits;
    vector = copy.vector;
  }

  public TritVector(final int size)
  {
    this.size = size;
    valueTrits = size;
    vector = new TritVectorBuffer(size);
  }

  public TritVector(final int[] trits)
  {
    this(trits.length);

    for (int i = 0; i < size; i++)
    {
      vector.buffer[i] = "-01".charAt(trits[i] + 1);
    }
  }

  public TritVector(final String trits)
  {
    this(trits.length());

    for (int i = 0; i < size; i++)
    {
      vector.buffer[i] = trits.charAt(i);
    }
  }

  public TritVector(final int size, final char trit)
  {
    this.size = size;

    switch (trit)
    {
    case '@':
      vector = nulls;
      break;

    case '0':
      vector = zeroes;
      valueTrits = size;
      break;

    case '-':
    case '1':
      if (size == 1)
      {
        vector = singleTrits;
        offset = trit == '1' ? 1 : 0;
        valueTrits = 1;
        return;
      }

    default:
      throw new CodeException("Undefined initialization trit");
    }

    vector.grow(size);
    while (vector.used < vector.buffer.length)
    {
      vector.buffer[vector.used++] = trit;
    }
  }

  private TritVector(final TritVector lhs, final TritVector rhs)
  {
    this(lhs.size() + rhs.size());

    copy(lhs, 0);
    copy(rhs, lhs.size());

    valueTrits = lhs.valueTrits + rhs.valueTrits;
  }

  public static TritVector concat(final TritVector lhs, final TritVector rhs)
  {
    if (lhs == null)
    {
      return rhs;
    }

    if (rhs == null)
    {
      return lhs;
    }

    // can we directly concatenate in lhs vector?
    if (lhs.offset + lhs.size() != lhs.vector.used || lhs.vector == nulls || lhs.vector == zeroes)
    {
      // nope, construct new vector

      // combine two null vectors?
      if (lhs.isNull() && rhs.isNull())
      {
        return new TritVector(lhs.size() + rhs.size(), '@');
      }

      // combine two zero vectors?
      if (lhs.vector == zeroes && rhs.vector == zeroes)
      {
        return new TritVector(lhs.size() + rhs.size(), '0');
      }

      return new TritVector(lhs, rhs);
    }

    // grow vector if necessary
    lhs.vector.grow(lhs.vector.used + rhs.size());

    // concatenate into lhs vector
    lhs.copy(rhs, lhs.vector.used);
    lhs.vector.used += rhs.size();

    // point to the new combined vector
    final TritVector result = new TritVector(lhs);
    result.size += rhs.size();
    result.valueTrits += rhs.valueTrits;
    return result;
  }

  public static TritVector fromTrytes(final String trytes)
  {
    final TritVector result = new TritVector(trytes.length() * 3);

    int offset = 0;
    for (int i = 0; i < trytes.length(); i++)
    {
      final int index = TritConverter.TRYTES.indexOf(trytes.charAt(i));
      final String trits = TritConverter.TRYTE_TRITS[index];
      for (int j = 0; j < 3; j++)
      {
        result.vector.buffer[offset + j] = trits.charAt(j);
      }

      offset += 3;
    }

    return result;
  }

  private void copy(final TritVector src, final int to)
  {
    for (int i = 0; i < src.size(); i++)
    {
      vector.buffer[to + i] = src.trit(i);
    }
  }

  @Override
  public boolean equals(final Object o)
  {
    if (!(o instanceof TritVector))
    {
      return false;
    }

    final TritVector rhs = (TritVector) o;
    if (size() != rhs.size())
    {
      return false;
    }

    for (int i = 0; i < size(); i++)
    {
      if (trit(i) != rhs.trit(i))
      {
        return false;
      }
    }

    return true;
  }

  public boolean isNull()
  {
    return valueTrits == 0;
  }

  public boolean isValue()
  {
    return valueTrits == size();
  }

  public boolean isZero()
  {
    if (vector == zeroes)
    {
      return true;
    }

    if (!isValue())
    {
      return false;
    }

    for (int i = 0; i < size(); i++)
    {
      if (trit(i) != '0')
      {
        return false;
      }
    }

    return true;
  }

  public int size()
  {
    return size;
  }

  public TritVector slice(final int start, final int length)
  {
    if (start < 0 || length < 0 || start + length > size())
    {
      throw new CodeException("Index out of range");
    }

    if (start == 0 && length == size())
    {
      // slice the entire vector
      return this;
    }

    final TritVector result = new TritVector(this);
    result.offset += start;
    result.size = length;
    if (isValue())
    {
      result.valueTrits = length;
      return result;
    }

    if (isNull())
    {
      return result;
    }

    // have to count non-null trits
    for (int i = 0; i < result.size(); i++)
    {
      if (result.trit(i) != '@')
      {
        result.valueTrits++;
      }
    }

    return result;
  }

  public TritVector slicePadded(final int start, final int length)
  {
    // slices trit vector as if it was padded with infinite zeroes

    if (start + length <= size())
    {
      // completely within range, normal slice
      return slice(start, length);
    }

    if (start >= size())
    {
      // completely outside range, just zeroes
      return new TritVector(length, '0');
    }

    final int remain = size() - start;
    final TritVector paddedZeroes = new TritVector(length - remain, '0');
    return TritVector.concat(slice(start, remain), paddedZeroes);
  }

  public String toDecimal()
  {
    return TritConverter.toDecimal(trits()).toString();
  }

  @Override
  public String toString()
  {
    final String varName = name != null ? name + ": " : "";
    return varName + "(" + toDecimal() + ") " + trits();
  }

  public String toTrytes()
  {
    final char[] buffer = new char[(size + 2) / 3];
    int start = offset;
    final char[] trits = new char[3];
    final int trytes = size / 3;
    for (int i = 0; i < trytes; i++)
    {
      for (int j = 0; j < 3; j++)
      {
        trits[j] = vector.buffer[start + j];
      }

      buffer[i] = tryte(trits);
      start += 3;
    }

    if (buffer.length > trytes)
    {
      // do remaining 1 or 2 trits
      trits[1] = '0';
      trits[2] = '0';
      final int end = trytes * 3;
      for (int i = end; i < size; i++)
      {
        trits[i - end] = vector.buffer[offset + i];
      }

      buffer[trytes] = tryte(trits);
    }

    return new String(buffer);
  }

  public char trit(final int index)
  {
    if (index < 0 || index >= size())
    {
      throw new CodeException("Index out of range");
    }

    return vector.buffer[offset + index];
  }

  public String trits()
  {
    return new String(vector.buffer, offset, size());
  }

  private char tryte(final char[] trits)
  {
    // unroll 3-char loop with multiplications

    int value = 13;
    switch (trits[0])
    {
    case '-':
      value--;
      break;

    case '1':
      value++;
      break;
    }

    switch (trits[1])
    {
    case '-':
      value -= 3;
      break;

    case '1':
      value += 3;
      break;
    }

    switch (trits[2])
    {
    case '-':
      value -= 9;
      break;

    case '1':
      value += 9;
      break;
    }

    return TritConverter.TRYTES.charAt(value);
  }

  static
  {
    singleTrits.buffer[0] = '-';
    singleTrits.buffer[1] = '1';
  }
}
