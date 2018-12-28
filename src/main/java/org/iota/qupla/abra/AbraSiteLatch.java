package org.iota.qupla.abra;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.context.CodeContext;

public class AbraSiteLatch extends AbraSite
{
  public AbraSite latch;

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append("latch " + name + "[" + size + "]");
  }

  @Override
  public void eval(final AbraCodeContext context)
  {
    context.evalLatch(this);
  }
}
