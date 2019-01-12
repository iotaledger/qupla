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

  @Override
  public String toString()
  {
    return super.toString() + "[]";
  }
}
