package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraSiteLatch extends AbraBaseSite
{
  public AbraBaseSite latch;

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalLatch(this);
  }
}
