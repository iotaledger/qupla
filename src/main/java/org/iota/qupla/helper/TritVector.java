package org.iota.qupla.helper;

import java.math.BigInteger;
import java.util.ArrayList;

import org.iota.qupla.exception.CodeException;

public class TritVector
{
  private static String nullTrits = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@";
  private static final ArrayList<Integer> powerDigits = new ArrayList<>();
  private static final ArrayList<BigInteger> powers = new ArrayList<>();
  private static final BigInteger three = new BigInteger("3");
  private static String zeroTrits = "0000000000000000000000000000000000000000000000000";
  public String name;
  private String trits = "";
  private int valueTrits;

  public TritVector(final TritVector copy)
  {
    trits = copy.trits;
    valueTrits = copy.valueTrits;
  }

  public TritVector(final String trits)
  {
    this.trits = trits;
    this.valueTrits = trits.length();
  }

  public TritVector(final int size, final char trit)
  {
    switch (trit)
    {
    case '@':
      trits = nulls(size);
      return;

    case '0':
      trits = zeroes(size);
      valueTrits = size;
      return;

    case '-':
    case '1':
      if (size == 1)
      {
        trits = "" + trit;
        valueTrits = 1;
        return;
      }
      break;
    }

    throw new CodeException(null, "Undefined initialization trit");
  }

  // concatenation constructor
  public TritVector(final TritVector lhs, final TritVector rhs)
  {
    trits = lhs.trits + rhs.trits;
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

    return new TritVector(lhs, rhs);
  }

  public static String nulls(final int size)
  {
    while (size > nullTrits.length())
    {
      nullTrits += nullTrits;
    }

    return nullTrits.substring(0, size);
  }

  public static String zeroes(final int size)
  {
    while (size > zeroTrits.length())
    {
      zeroTrits += zeroTrits;
    }

    return zeroTrits.substring(0, size);
  }

  public String display(final int mantissa, final int exponent)
  {
    final String varName = name != null ? name + ": " : "";
    if (valueTrits == trits.length())
    {
      return varName + "(" + displayValue(mantissa, exponent) + ") " + trits;
    }

    if (valueTrits == 0)
    {
      return varName + "(NULL) " + trits;
    }

    return varName + "(***SOME NULL TRITS***) " + trits;
  }

  public String displayValue(final int mantissa, final int exponent)
  {
    if (exponent > 0 && mantissa > 0)
    {
      return TritConverter.toFloat(trits(), mantissa, exponent);
    }

    return TritConverter.toDecimal(trits()).toString();
  }

  @Override
  public boolean equals(final Object o)
  {
    return o instanceof TritVector && trits.equals(((TritVector) o).trits);
  }

  public boolean isNull()
  {
    return valueTrits == 0;
  }

  public boolean isValue()
  {
    return valueTrits == trits.length();
  }

  public boolean isZero()
  {
    if (!isValue())
    {
      return false;
    }

    for (int i = 0; i < trits.length(); i++)
    {
      if (trits.charAt(i) != '0')
      {
        return false;
      }
    }

    return true;
  }

  public int size()
  {
    return trits.length();
  }

  public TritVector slice(final int start, final int size)
  {
    if (start == 0 && size == trits.length())
    {
      return this;
    }

    final TritVector result = new TritVector(trits.substring(start, start + size));
    if (valueTrits == trits.length())
    {
      return result;
    }

    result.valueTrits = 0;
    if (valueTrits == 0)
    {
      return result;
    }

    // have to count non-null trits
    for (int i = 0; i < result.trits.length(); i++)
    {
      if (result.trits.charAt(i) != '@')
      {
        result.valueTrits++;
      }
    }

    return result;
  }

  public TritVector slicePadded(final int start, final int size)
  {
    // pads slice with zeroes if necessary

    if (start + size <= trits.length())
    {
      return slice(start, size);
    }

    if (start >= trits.length())
    {
      return new TritVector(size, '0');
    }

    final int remain = trits.length() - start;
    final TritVector ret = slice(start, remain);
    ret.trits += zeroes(size - remain);
    ret.valueTrits = size;
    return ret;
  }

  @Override
  public String toString()
  {
    return display(0, 0);
  }

  public char trit(final int index)
  {
    return trits.charAt(index);
  }

  public String trits()
  {
    return trits;
  }
}
