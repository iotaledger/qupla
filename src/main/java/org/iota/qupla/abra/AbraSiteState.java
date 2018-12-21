package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;

public class AbraSiteState extends AbraSite
{
  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append("state " + name + "[" + size + "]");
  }
}
