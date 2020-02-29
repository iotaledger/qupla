package org.iota.qupla.helper;

import java.util.Arrays;

import org.iota.qupla.exception.CodeException;

public class TritVector
{
  public static final byte TRIT_FALSE;
  public static final byte TRIT_MIN = (byte) '-';
  public static final byte TRIT_NULL = (byte) '@';
  public static final byte TRIT_ONE = (byte) '1';
  public static final byte TRIT_TRUE;
  public static final byte TRIT_ZERO = (byte) '0';
  private static byte[] BITS_TO_TRIT = {
      TRIT_NULL,
      TRIT_ONE,
      TRIT_MIN,
      TRIT_ZERO
  };
  private static final byte[] INT_TO_TRIT = {
      TRIT_MIN,
      TRIT_ZERO,
      TRIT_ONE
  };
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
      vector.buffer[i] = INT_TO_TRIT[trits[i] + 1];
    }
  }

  public TritVector(final byte[] trits)
  {
    this(trits.length);

    System.arraycopy(trits, 0, vector.buffer, 0, size);
  }

  public TritVector(final String trits)
  {
    this(trits.length());

    for (int i = 0; i < size; i++)
    {
      switch (trits.charAt(i))
      {
      case '0':
        vector.buffer[i] = TritVector.TRIT_ZERO;
        break;
      case '1':
        vector.buffer[i] = TritVector.TRIT_ONE;
        break;
      case '-':
        vector.buffer[i] = TritVector.TRIT_MIN;
        break;
      case '@':
        vector.buffer[i] = TritVector.TRIT_NULL;
        break;
      }
    }
  }

  public TritVector(final int size, final byte trit)
  {
    this.size = size;

    switch (trit)
    {
    case TRIT_NULL:
      vector = nulls;
      break;

    case TRIT_ZERO:
      vector = zeroes;
      valueTrits = size;
      break;

    case TRIT_MIN:
    case TRIT_ONE:
      if (size == 1)
      {
        vector = singleTrits;
        offset = trit == TRIT_ONE ? 1 : 0;
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

  public static byte bitsToTrit(final byte bits)
  {
    return BITS_TO_TRIT[bits];
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
        return new TritVector(lhs.size() + rhs.size(), TRIT_NULL);
      }

      // combine two zero vectors?
      if (lhs.vector == zeroes && rhs.vector == zeroes)
      {
        return new TritVector(lhs.size() + rhs.size(), TRIT_ZERO);
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
      final byte[] tryteTrits = TritConverter.TRYTE_TRITS[index];
      System.arraycopy(tryteTrits, 0, result.vector.buffer, offset, 3);

      offset += 3;
    }

    return result;
  }

  public static byte tritToBits(final byte trit)
  {
    switch (trit)
    {
    case TRIT_MIN:
      return 2;
    case TRIT_NULL:
      return 0;
    case TRIT_ONE:
      return 1;
    case TRIT_ZERO:
      return 3;
    }

    throw new IllegalArgumentException("Trit range");
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
      if (trit(i) != TRIT_ZERO)
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
      throw new CodeException("Slice out of range (" + size() + "): " + start + ":" + length);
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
    result.valueTrits = 0;
    for (int i = 0; i < result.size(); i++)
    {
      if (result.trit(i) != TRIT_NULL)
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
      return new TritVector(length, TRIT_ZERO);
    }

    final int remain = size() - start;
    final TritVector paddedZeroes = new TritVector(length - remain, TRIT_ZERO);
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
    return varName + "(" + toDecimal() + ") " + new String(trits());
  }

  public String toTrytes()
  {
    final byte[] buffer = new byte[(size + 2) / 3];
    int start = offset;
    final byte[] trits = new byte[3];
    final int trytes = size / 3;
    for (int i = 0; i < trytes; i++)
    {
      System.arraycopy(vector.buffer, start, trits, 0, 3);
      buffer[i] = tryte(trits);
      start += 3;
    }

    if (buffer.length > trytes)
    {
      // do remaining 1 or 2 trits
      trits[1] = TRIT_ZERO;
      trits[2] = TRIT_ZERO;
      final int end = trytes * 3;
      System.arraycopy(vector.buffer, offset + end, trits, 0, size - end);
      buffer[trytes] = tryte(trits);
    }

    return new String(buffer);
  }

  public byte trit(final int index)
  {
    if (index < 0 || index >= size())
    {
      throw new CodeException("Index out of range");
    }

    return vector.buffer[offset + index];
  }

  public byte[] trits()
  {
    return Arrays.copyOfRange(vector.buffer, offset, offset + size());
  }

  private byte tryte(final byte[] trits)
  {
    // unroll 3-char loop with multiplications

    int value = 13;
    switch (trits[0])
    {
    case TRIT_MIN:
      value--;
      break;

    case TRIT_ONE:
      value++;
      break;
    }

    switch (trits[1])
    {
    case TRIT_MIN:
      value -= 3;
      break;

    case TRIT_ONE:
      value += 3;
      break;
    }

    switch (trits[2])
    {
    case TRIT_MIN:
      value -= 9;
      break;

    case TRIT_ONE:
      value += 9;
      break;
    }

    return (byte) TritConverter.TRYTES.charAt(value);
  }

  static
  {
    TRIT_FALSE = TritConverter.FALSE_IS_MIN ? TRIT_MIN : TRIT_ZERO;
    TRIT_TRUE = TRIT_ONE;
    singleTrits.buffer[0] = TRIT_MIN;
    singleTrits.buffer[1] = TRIT_ONE;
  }
}
