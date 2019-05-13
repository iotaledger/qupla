package org.iota.qupla.abra.block;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;

//TODO merge identical LUTs
public class AbraBlockLut extends AbraBaseBlock
{
  public static final String NULL_LUT = "@@@@@@@@@@@@@@@@@@@@@@@@@@@";
  public String lookup = NULL_LUT;

  public static String unnamed(final String lookupTable)
  {
    return "lut_" + lookupTable.replace('-', 'T').replace('@', 'N');
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalLut(this);
  }

  public int inputs()
  {
    // determine how many inputs are significant
    if (lookup.startsWith(lookup.substring(9, 18)) && lookup.startsWith(lookup.substring(18, 27)))
    {
      if (lookup.startsWith(lookup.substring(3, 6)) && lookup.startsWith(lookup.substring(6, 9)))
      {
        return 1;
      }

      return 2;
    }

    return 3;
  }

  @Override
  public String toString()
  {
    return name + "[]";
  }
}
