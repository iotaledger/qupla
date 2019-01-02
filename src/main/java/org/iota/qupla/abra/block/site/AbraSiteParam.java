package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraSiteParam extends AbraBaseSite
{
  public int offset;

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalParam(this);
  }
}
