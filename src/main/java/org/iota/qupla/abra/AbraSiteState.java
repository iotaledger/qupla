package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;

public class AbraSiteState extends AbraSite
{
  public AbraSite latch;

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append("state " + name + "[" + size + "]");
  }
}
