package org.iota.qupla.helper;

import java.math.BigInteger;
import java.util.ArrayList;

public class TritVector
{
  private static String nullTrits = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@";
  private static final ArrayList<Integer> powerDigits = new ArrayList<>();
  private static final ArrayList<BigInteger> powers = new ArrayList<>();
  private static final BigInteger three = new BigInteger("3");
  private static String zeroTrits = "0000000000000000000000000000000000000000000000000";
  public String name;
  public String trits = "";
  public int valueTrits;

  public TritVector()
  {
  }

  public TritVector(final TritVector copy)
  {
    trits = copy.trits;
    valueTrits = copy.valueTrits;
  }

  public TritVector(final String trits, final int valueTrits)
  {
    this.trits = trits;
    this.valueTrits = valueTrits;
  }

  public TritVector(int size)
  {
    while (size > nullTrits.length())
    {
      nullTrits += nullTrits;
    }

    trits = nullTrits.substring(0, size);
  }

  // concatenation constructor
  public TritVector(final TritVector lhs, final TritVector rhs)
  {
    trits = lhs.trits + rhs.trits;
    valueTrits = lhs.valueTrits + rhs.valueTrits;
  }

  public static String zero(final int size)
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
      return toFloat(mantissa, exponent);
    }

    return toDecimal().toString();
  }

  public TritVector extract(final int start, final int size)
  {
    // pads slice with zeroes if necessary

    if (start + size <= trits.length())
    {
      return slice(start, size);
    }

    if (start >= trits.length())
    {
      return new TritVector(zero(size), size);
    }

    final int remain = trits.length() - start;
    final TritVector ret = slice(start, remain);
    ret.trits += zero(size - remain);
    ret.valueTrits = size;
    return ret;
  }

  public void fromDecimal(final String decimal)
  {
    if (decimal.length() == 1 && decimal.charAt(0) < '2')
    {
      trits = decimal;
      valueTrits = trits.length();
      return;
    }

    // take abs(name)
    final boolean negative = decimal.startsWith("-");
    final String value = negative ? decimal.substring(1) : decimal;

    // convert to unbalanced ternary
    final char[] buffer = new char[value.length() * 3];
    final char[] quotient = value.toCharArray();
    for (int i = 0; i < quotient.length; i++)
    {
      quotient[i] -= '0';
    }

    int qLength = quotient.length;

    int bLength = 0;
    while (qLength != 1 || quotient[0] != 0)
    {
      int vLength = qLength;
      qLength = 0;
      char digit = quotient[0];
      if (digit >= 3 || vLength == 1)
      {
        quotient[qLength++] = (char) (digit / 3);
        digit %= 3;
      }

      for (int index = 1; index < vLength; index++)
      {
        digit = (char) (digit * 10 + quotient[index]);
        quotient[qLength++] = (char) (digit / 3);
        digit %= 3;
      }

      buffer[bLength++] = digit;
    }

    // convert unbalanced to balanced ternary
    // note that we negate the result if necessary in the same pass
    int carry = 0;
    for (int i = 0; i < bLength; i++)
    {
      switch (buffer[i] + carry)
      {
      case 0:
        buffer[i] = '0';
        carry = 0;
        break;

      case 1:
        buffer[i] = negative ? '-' : '1';
        carry = 0;
        break;

      case 2:
        buffer[i] = negative ? '1' : '-';
        carry = 1;
        break;

      case 3:
        buffer[i] = '0';
        carry = 1;
        break;
      }
    }

    if (carry != 0)
    {
      buffer[bLength++] = negative ? '-' : '1';
    }

    trits = new String(buffer, 0, bLength);
    valueTrits = bLength;
  }

  public String fromFloat(final String value, final int manSize, final int expSize)
  {
    final int dot = value.indexOf('.');
    if (dot < 0)
    {
      // handle integer constant

      if (value.equals("0"))
      {
        // special case: both mantissa and exponent zero
        valueTrits = manSize + expSize;
        trits = zero(valueTrits);
        return null;
      }

      // get minimum trit vector that represents integer
      fromDecimal(value);

      // make sure it fits in the mantissa
      if (trits.length() > manSize)
      {
        return "Mantissa '" + value + "' exceeds " + manSize + " trits";
      }

      // shift all significant trits to normalize
      final String mantissa = zero(manSize - trits.length()) + trits;
      return makeFloat(mantissa, trits.length(), expSize);
    }

    // handle float constant

    // use BigInteger arithmetic to convert the value
    // <integer> * 10^-<decimals> * 3^0
    // into the following without losing too much precision
    // <ternary> * 10^0 * 3^<exponent>
    // we do that by calculating a minimum necessary ternary exponent,
    // then multiply by that <exponent> and divide by 10^<decimals>
    // after that it becomes a matter of normalizing and rounding the
    // ternary representation of the result

    final int decimals = value.length() - dot - 1;
    final String integer = value.substring(0, dot) + value.substring(dot + 1);
    final BigInteger intValue = new BigInteger(integer);
    final BigInteger tenPower = new BigInteger("1" + zero(decimals));

    // do a rough estimate of how many trits we will need at a minimum
    // add at least 20 trits of wiggling room to reduce rounding errors
    // also: calculate 3 trits per decimal just to be sure
    int exponent = -(manSize + 20 + 3 * decimals);
    final BigInteger ternary = intValue.multiply(getPower(-exponent)).divide(tenPower);
    fromDecimal(ternary.toString());

    // take <manSize> most significant trits
    final String mantissa = trits.substring(trits.length() - manSize);
    return makeFloat(mantissa, exponent + trits.length(), expSize);
  }

  public void fromLong(final long decimal)
  {
    //TODO replace this inefficient lazy-ass code :-P
    fromDecimal("" + decimal);
  }

  private BigInteger getPower(final int nr)
  {
    if (nr >= powers.size())
    {
      if (powers.size() == 0)
      {
        powers.add(new BigInteger("1"));
        powerDigits.add(1);
      }

      BigInteger big = powers.get(powers.size() - 1);
      for (int i = powers.size(); i <= nr; i++)
      {
        big = big.multiply(three);
        powers.add(big);
        powerDigits.add(big.toString().length());
      }
    }

    return powers.get(nr);
  }

  public boolean isZero()
  {
    if (valueTrits != trits.length())
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

  private String makeFloat(final String mantissa, final int exponent, final int expSize)
  {
    fromLong(exponent);

    // make sure exponent fits
    if (trits.length() > expSize)
    {
      return "Exponent '" + exponent + "' exceeds " + expSize + " trits";
    }

    if (trits.length() < expSize)
    {
      padZero(expSize);
    }

    trits = mantissa + trits;
    valueTrits = trits.length();
    return null;
  }

  public void padZero(final int size)
  {
    trits += zero(size - trits.length());
  }

  public TritVector slice(final int start, final int size)
  {
    if (start == 0 && size == trits.length())
    {
      return this;
    }

    final TritVector result = new TritVector();
    result.trits = trits.substring(start, start + size);
    if (valueTrits != 0)
    {
      if (valueTrits != trits.length())
      {
        // have to count values
        for (int i = 0; i < result.trits.length(); i++)
        {
          result.valueTrits += result.trits.charAt(i) != '@' ? 1 : 0;
        }

        return result;
      }

      result.valueTrits = result.trits.length();
    }

    return result;
  }

  public BigInteger toDecimal()
  {
    BigInteger result = new BigInteger("0");
    for (int i = 0; i < trits.length(); i++)
    {
      final char c = trits.charAt(i);
      if (c != '0')
      {
        final BigInteger power = getPower(i);
        result = c == '-' ? result.subtract(power) : result.add(power);
      }
    }

    return result;
  }

  private String toFloat(final int manSize, final int expSize)
  {
    // find first significant trit
    int significant = 0;
    while (significant < manSize && trits.charAt(significant) == '0')
    {
      significant++;
    }

    // special case: all zero trits in mantissa
    if (significant == manSize)
    {
      return "0.0";
    }

    // shift the significant trits of the mantissa to the left to get
    // its integer representation (we will need to correct the exponent)
    final TritVector mantissa = new TritVector(trits.substring(significant, manSize), manSize - significant);
    final BigInteger integer = mantissa.toDecimal();

    // get exponent and correct with mantissa shift factor
    final TritVector exp = new TritVector(trits.substring(manSize), expSize);
    int exponent = (int) exp.toLong() - mantissa.trits.length();
    if (exponent == 0)
    {
      // simple case: 3^0 equals 1, just return integer
      return integer + ".0";
    }

    if (exponent > 0)
    {
      return integer.multiply(getPower(exponent)) + ".0";
    }

    return toFloatWithFraction(integer, exponent, manSize);
  }

  private String toFloatWithFraction(final BigInteger integer, final int exponent, final int manSize)
  {
    if (integer.signum() < 0)
    {
      return "-" + toFloatWithFraction(integer.negate(), exponent, manSize);
    }

    getPower(manSize);
    final int digits = powerDigits.get(manSize) - 1;

    final int lhsLength = integer.toString().length();
    final BigInteger power = getPower(-exponent);
    final int rhsLength = powerDigits.get(-exponent);
    final int extra = lhsLength < rhsLength ? rhsLength - lhsLength : 0;
    final BigInteger mul = integer.multiply(new BigInteger("1" + zero(digits + extra)));
    final BigInteger div = mul.divide(power);
    final String divResult = div.toString();
    final int decimal = divResult.length() - digits - extra;
    int last = divResult.length() - 1;
    while (last > 0 && last > decimal && divResult.charAt(last - 1) == '0')
    {
      last--;
    }

    if (decimal < 0)
    {
      return "0." + zero(-decimal) + divResult.substring(0, last);
    }

    final String fraction = last == decimal ? "0" : divResult.substring(decimal, last);
    if (decimal == 0)
    {
      return "0." + fraction;
    }

    return divResult.substring(0, decimal) + "." + fraction;
  }

  public long toLong()
  {
    long result = 0;
    long power = 1;
    for (int i = 0; i < trits.length(); i++)
    {
      final char c = trits.charAt(i);
      if (c != '0')
      {
        result += c == '-' ? -power : power;
      }

      power *= 3;
    }

    return result;
  }

  public int toLutIndex()
  {
    if (valueTrits != trits.length())
    {
      return -1;
    }

    int index = 0;
    for (int i = 0; i < trits.length(); i++)
    {
      index *= 3;
      final char c = trits.charAt(i);
      if (c != '0')
      {
        index += c == '1' ? 1 : 2;
      }
    }

    return index;
  }

  @Override
  public String toString()
  {
    return display(0, 0);
  }
}
