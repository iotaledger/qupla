package org.iota.qupla.abra.block;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;

//TODO merge identical LUTs
public class AbraBlockLut extends AbraBaseBlock
{
  public String lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@";
  public int tritNr;

  @Override
  public boolean anyNull()
  {
    return true;
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalLut(this);
  }

  @Override
  public String type()
  {
    return "[]";
  }
}
