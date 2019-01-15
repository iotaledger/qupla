package org.iota.qupla.helper;

import java.math.BigInteger;
import java.util.ArrayList;

import org.iota.qupla.exception.CodeException;

public class TritConverter
{
  private static final ArrayList<Integer> powerDigits = new ArrayList<>();
  private static final ArrayList<BigInteger> powers = new ArrayList<>();
  private static final BigInteger three = new BigInteger("3");
  public static final String[] tryteValue = {
      "---",
      "0--",
      "1--",
      "-0-",
      "00-",
      "10-",
      "-1-",
      "01-",
      "11-",
      "--0",
      "0-0",
      "1-0",
      "-00",
      "000",
      "100",
      "-10",
      "010",
      "110",
      "--1",
      "0-1",
      "1-1",
      "-01",
      "001",
      "101",
      "-11",
      "011",
      "111"
  };

  public static String fromDecimal(final String decimal)
  {
    if (decimal.length() == 1 && decimal.charAt(0) < '2')
    {
      return decimal;
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

    return new String(buffer, 0, bLength);
  }

  public static String fromFloat(final String value, final int manSize, final int expSize)
  {
    final int dot = value.indexOf('.');
    if (dot < 0)
    {
      // handle integer constant

      if (value.equals("0"))
      {
        // special case: both mantissa and exponent zero
        return zeroes(manSize + expSize);
      }

      // get minimum trit vector that represents integer
      final String trits = fromDecimal(value);

      // make sure it fits in the mantissa
      if (trits.length() > manSize)
      {
        throw new CodeException("Mantissa '" + value + "' exceeds " + manSize + " trits");
      }

      // shift all significant trits to normalize
      final String mantissa = zeroes(manSize - trits.length()) + trits;
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
    final BigInteger tenPower = new BigInteger("1" + zeroes(decimals));

    // do a rough estimate of how many trits we will need at a minimum
    // add at least 20 trits of wiggling room to reduce rounding errors
    // also: calculate 3 trits per decimal just to be sure
    int exponent = -(manSize + 20 + 3 * decimals);
    final BigInteger ternary = intValue.multiply(getPower(-exponent)).divide(tenPower);
    final String trits = fromDecimal(ternary.toString());

    // take <manSize> most significant trits
    final String mantissa = trits.substring(trits.length() - manSize);
    return makeFloat(mantissa, exponent + trits.length(), expSize);
  }

  public static String fromLong(final long decimal)
  {
    //TODO replace this inefficient lazy-ass code :-P
    return fromDecimal("" + decimal);
  }

  private static BigInteger getPower(final int nr)
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

  private static String makeFloat(final String mantissa, final int exponent, final int expSize)
  {
    final String trits = fromLong(exponent);

    // make sure exponent fits
    if (trits.length() > expSize)
    {
      throw new CodeException("Exponent '" + exponent + "' exceeds " + expSize + " trits");
    }

    return mantissa + trits + zeroes(expSize - trits.length());
  }

  public static BigInteger toDecimal(final String trits)
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

  //TODO expSize is never used???
  public static String toFloat(final String trits, final int manSize, final int expSize)
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
    final String mantissa = trits.substring(significant, manSize);
    final BigInteger integer = toDecimal(mantissa);

    // get exponent and correct with mantissa shift factor
    int exponent = TritConverter.toInt(trits.substring(manSize)) - mantissa.length();
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

  private static String toFloatWithFraction(final BigInteger integer, final int exponent, final int manSize)
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
    final BigInteger mul = integer.multiply(new BigInteger("1" + zeroes(digits + extra)));
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
      return "0." + zeroes(-decimal) + divResult.substring(0, last);
    }

    final String fraction = last == decimal ? "0" : divResult.substring(decimal, last);
    if (decimal == 0)
    {
      return "0." + fraction;
    }

    return divResult.substring(0, decimal) + "." + fraction;
  }

  public static int toInt(final String trits)
  {
    int result = 0;
    int power = 1;
    for (int i = 0; i < trits.length(); i++)
    {
      final char trit = trits.charAt(i);
      if (trit != '0')
      {
        result += trit == '-' ? -power : power;
      }

      power *= 3;
    }

    return result;
  }

  private static String zeroes(final int size)
  {
    return new TritVector(size, '0').trits();
  }
}
