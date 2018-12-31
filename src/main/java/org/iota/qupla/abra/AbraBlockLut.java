package org.iota.qupla.abra;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.context.CodeContext;

//TODO merge identical LUTs
public class AbraBlockLut extends AbraBlock
{
  public String lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@";
  public int tritNr;

  @Override
  public boolean anyNull()
  {
    return true;
  }

  @Override
  public CodeContext append(final CodeContext context)
  {
    context.append("// lut block " + index);
    context.append(" // " + lookup);
    return context.append(" // " + name + type()).newline();
  }

  @Override
  public void code()
  {
    if (tritCode.bufferOffset > 0)
    {
      return;
    }

    //TODO convert 27 bct lookup 'trits' to 35 trits and put in tritCode
  }

  @Override
  public void eval(final AbraCodeContext context)
  {
    context.evalLut(this);
  }

  @Override
  public String type()
  {
    return "[]";
  }
}
