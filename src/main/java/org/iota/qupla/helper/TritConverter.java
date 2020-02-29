package org.iota.qupla.helper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import org.iota.qupla.exception.CodeException;

public class TritConverter
{
  public static final String BOOL_FALSE;
  public static final String BOOL_TRUE;
  public static final boolean FALSE_IS_MIN = true;
  public static final String TRYTES = "NOPQRSTUVWXYZ9ABCDEFGHIJKLM";
  public static final byte[][] TRYTE_TRITS;
  private static final ArrayList<Integer> powerDigits = new ArrayList<>();
  private static final ArrayList<BigInteger> powers = new ArrayList<>();
  private static final BigInteger three = new BigInteger("3");

  public static byte[] fromDecimal(final String decimal)
  {
    if (decimal.length() == 1)
    {
      switch (decimal.charAt(0))
      {
      case '0':
        return new byte[] { TritVector.TRIT_ZERO };
      case '1':
        return new byte[] { TritVector.TRIT_ONE };
      case '-':
        return new byte[] { TritVector.TRIT_MIN };
      }
    }

    // take abs(name)
    final boolean negative = decimal.startsWith("-");
    final String value = negative ? decimal.substring(1) : decimal;

    // convert to unbalanced ternary
    final byte[] buffer = new byte[value.length() * 3];
    final byte[] quotient = value.getBytes();
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
      byte digit = quotient[0];
      if (digit >= 3 || vLength == 1)
      {
        quotient[qLength++] = (byte) (digit / 3);
        digit %= 3;
      }

      for (int index = 1; index < vLength; index++)
      {
        digit = (byte) (digit * 10 + quotient[index]);
        quotient[qLength++] = (byte) (digit / 3);
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
        buffer[i] = TritVector.TRIT_ZERO;
        carry = 0;
        break;

      case 1:
        buffer[i] = negative ? TritVector.TRIT_MIN : TritVector.TRIT_ONE;
        carry = 0;
        break;

      case 2:
        buffer[i] = negative ? TritVector.TRIT_ONE : TritVector.TRIT_MIN;
        carry = 1;
        break;

      case 3:
        buffer[i] = TritVector.TRIT_ZERO;
        carry = 1;
        break;
      }
    }

    if (carry != 0)
    {
      buffer[bLength++] = negative ? TritVector.TRIT_MIN : TritVector.TRIT_ONE;
    }

    return Arrays.copyOf(buffer, bLength);
  }

  public static byte[] fromFloat(final String value, final int manSize, final int expSize)
  {
    final int dot = value.indexOf('.');
    if (dot < 0)
    {
      // handle integer constant

      if (value.equals("0"))
      {
        // special case: both mantissa and exponent zero
        final byte[] result = new byte[manSize + expSize];
        Arrays.fill(result, TritVector.TRIT_ZERO);
        return result;
      }

      // get minimum trit vector that represents integer
      final byte[] trits = fromDecimal(value);

      // make sure it fits in the mantissa
      if (trits.length > manSize)
      {
        throw new CodeException("Mantissa '" + value + "' exceeds " + manSize + " trits");
      }

      // shift all significant trits to normalize
      int zeroes = manSize - trits.length;
      final byte[] mantissa = new byte[manSize];
      Arrays.fill(mantissa, 0, zeroes, TritVector.TRIT_ZERO);
      System.arraycopy(trits, 0, mantissa, zeroes, trits.length);
      return makeFloat(mantissa, trits.length, expSize);
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
    final byte[] trits = fromDecimal(ternary.toString());

    // take <manSize> most significant trits
    final byte[] mantissa = Arrays.copyOfRange(trits, trits.length - manSize, trits.length);
    return makeFloat(mantissa, exponent + trits.length, expSize);
  }

  public static byte[] fromLong(final long decimal)
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

  private static byte[] makeFloat(final byte[] mantissa, final int exponent, final int expSize)
  {
    // make sure exponent fits
    final byte[] trits = fromLong(exponent);
    if (trits.length > expSize)
    {
      throw new CodeException("Exponent '" + exponent + "' exceeds " + expSize + " trits");
    }

    final byte[] result = Arrays.copyOf(mantissa, mantissa.length + expSize);
    System.arraycopy(trits, 0, result, mantissa.length, trits.length);
    Arrays.fill(result, mantissa.length + trits.length, result.length, TritVector.TRIT_ZERO);
    return result;
  }

  public static BigInteger toDecimal(final byte[] trits)
  {
    BigInteger result = new BigInteger("0");
    for (int i = 0; i < trits.length; i++)
    {
      final byte trit = trits[i];
      if (trit != TritVector.TRIT_ZERO)
      {
        final BigInteger power = getPower(i);
        result = trit == TritVector.TRIT_MIN ? result.subtract(power) : result.add(power);
      }
    }

    return result;
  }

  //TODO expSize is never used???
  public static String toFloat(final byte[] trits, final int manSize, final int expSize)
  {
    // find first significant trit
    int significant = 0;
    while (significant < manSize && trits[significant] == TritVector.TRIT_ZERO)
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
    final byte[] mantissa = Arrays.copyOfRange(trits, significant, manSize);
    final BigInteger integer = toDecimal(mantissa);

    // get exponent and correct with mantissa shift factor
    final byte[] exponentTrits = Arrays.copyOfRange(trits, manSize, trits.length);
    final int exponent = TritConverter.toInt(exponentTrits) - mantissa.length;
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

  public static int toInt(final byte[] trits)
  {
    int result = 0;
    int power = 1;
    for (final byte trit : trits)
    {
      if (trit != TritVector.TRIT_ZERO)
      {
        result += trit == TritVector.TRIT_MIN ? -power : power;
      }

      power *= 3;
    }

    return result;
  }

  public static long toLong(final byte[] trits)
  {
    long result = 0;
    long power = 1;
    for (final byte trit : trits)
    {
      if (trit != TritVector.TRIT_ZERO)
      {
        result += trit == TritVector.TRIT_MIN ? -power : power;
      }

      power *= 3;
    }

    return result;
  }

  public static String tritsToTrytes(final int[] trits)
  {
    final int size = trits.length / 3;
    final char[] buffer = new char[size];
    int offset = 0;
    for (int i = 0; i < size; i++)
    {
      final int index = trits[offset] + trits[offset + 1] * 3 + trits[offset + 2] * 9;
      buffer[i] = TRYTES.charAt(index + 13);
      offset += 3;
    }

    return new String(buffer);
  }

  public static TritVector tritsToVector(final int[] trits)
  {
    return new TritVector(trits);
  }

  public static int[] trytesToTrits(final String trytes)
  {
    final int[] result = new int[trytes.length() * 3];
    int offset = 0;
    for (int i = 0; i < trytes.length(); i++)
    {
      final int index = TRYTES.indexOf(trytes.charAt(i));
      final byte[] trits = TRYTE_TRITS[index];
      for (int j = 0; j < 3; j++)
      {
        switch (trits[j])
        {
        case TritVector.TRIT_ONE:
          result[offset + j] = 1;
          break;

        case TritVector.TRIT_MIN:
          result[offset + j] = -1;
          break;
        }
      }

      offset += 3;
    }

    return result;
  }

  public static TritVector trytesToVector(final String trytes)
  {
    return TritVector.fromTrytes(trytes);
  }

  public static int[] vectorToTrits(final TritVector vector)
  {
    final int[] result = new int[vector.size()];
    for (int i = 0; i < result.length; i++)
    {
      switch (vector.trit(i))
      {
      case TritVector.TRIT_ONE:
        result[i] = 1;
        break;

      case TritVector.TRIT_MIN:
        result[i] = -1;
        break;
      }
    }

    return result;
  }

  public static String vectorToTrytes(final TritVector vector)
  {
    return vector.toTrytes();
  }

  private static String zeroes(final int size)
  {
    final char[] buffer = new char[size];
    Arrays.fill(buffer, '0');
    return new String(buffer);
  }

  static
  {
    BOOL_FALSE = FALSE_IS_MIN ? "-" : "0";
    BOOL_TRUE = "1";

    TRYTE_TRITS = new byte[27][];
    for (int i = 0; i < 27; i++)
    {
      // make sure each value from -13 to +13 is at least 3 trits long
      // we achieve this by adding 27, so that each value is 4 trits
      final byte[] trits = fromLong(27 - 13 + i);

      // take only the first 3 trits
      TRYTE_TRITS[i] = Arrays.copyOf(trits, 3);
    }
  }
}
