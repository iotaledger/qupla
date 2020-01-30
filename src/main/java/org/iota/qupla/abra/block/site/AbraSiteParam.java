package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraSiteParam extends AbraBaseSite
{
  public int offset;

  public AbraSiteParam()
  {
  }

  public AbraSiteParam(final AbraSiteParam copy)
  {
    super(copy);
    offset = copy.offset;
  }

  @Override
  public AbraBaseSite clone()
  {
    return new AbraSiteParam(this);
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalParam(this);
  }

  @Override
  public boolean isIdentical(final AbraBaseSite rhs)
  {
    return false;
  }
}
