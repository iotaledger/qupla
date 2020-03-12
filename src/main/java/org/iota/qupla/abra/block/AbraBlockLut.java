package org.iota.qupla.abra.block;

import java.util.Arrays;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

//TODO merge identical LUTs
public class AbraBlockLut extends AbraBaseBlock
{
  private static final String LUT_FALSE_IS_MIN = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
  private static final String LUT_FALSE_IS_ZERO = "@-@@0@@1@@-@@0@@1@@-@@0@@1@";
  public static final String LUT_NULLIFY_FALSE = TritConverter.FALSE_IS_MIN ? LUT_FALSE_IS_MIN : LUT_FALSE_IS_ZERO;
  public static final String LUT_NULLIFY_TRUE = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";

  private byte[] lookup;

  public AbraBlockLut()
  {
    this(TritVector.TRIT_NULL);
  }

  public AbraBlockLut(byte filler)
  {
    lookup = new byte[27];
    Arrays.fill(lookup, filler);
  }

  public AbraBlockLut(byte[] lookup)
  {
    this.lookup = lookup;
  }

  public AbraBlockLut(final String table)
  {
    lookup = new TritVector(table).trits();
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalLut(this);
  }

  public void fromLong(long value)
  {
    for (int i = 0; i < 27; i++)
    {
      lookup[i] = TritVector.bitsToTrit((byte) (value & 0x03));
      value >>= 2;
    }

    name = unnamed();
  }

  public int inputs()
  {
    // check if the first 9 trits are repeated twice
    for (int i = 0; i < 9; i++)
    {
      if (lookup[i] != lookup[i + 9] || lookup[i] != lookup[i + 18])
      {
        // no repeat, 3 inputs are significant
        return 3;
      }
    }

    // 9 trits do repeat, now check if the first 3 trits are repeated twice
    for (int i = 0; i < 3; i++)
    {
      if (lookup[i] != lookup[i + 3] || lookup[i] != lookup[i + 6])
      {
        // no repeat, 2 inputs are significant
        return 2;
      }
    }

    // 3 trits do repeat, only 1 input is significant
    return 1;
  }

  public byte lookup(final int index)
  {
    return lookup[index];
  }

  public long toLong()
  {
    long value = 0;
    for (int i = 26; i >= 0; i--)
    {
      value = (value << 2) + TritVector.tritToBits(lookup[i]);
    }

    return value;
  }

  @Override
  public String toString()
  {
    return name + "[]";
  }

  public String toTable()
  {
    final char[] table = new char[27];
    for (int i = 0; i < 27; i++)
    {
      final byte bits = TritVector.tritToBits(lookup[i]);
      table[i] = "@1-0".charAt(bits);
    }

    return new String(table);
  }

  public String unnamed()
  {
    final char[] lutName = new char[27];
    for (int i = 0; i < 27; i++)
    {
      final byte bits = TritVector.tritToBits(lookup[i]);
      lutName[i] = "N1T0".charAt(bits);
    }

    return "lut_" + new String(lutName);
  }
}
