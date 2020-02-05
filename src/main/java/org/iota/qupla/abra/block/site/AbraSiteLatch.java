package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraSiteLatch extends AbraBaseSite
{
  public AbraBaseSite latchSite;

  public AbraSiteLatch()
  {
  }

  public AbraSiteLatch(final AbraSiteLatch copy)
  {
    super(copy);
  }

  @Override
  public AbraBaseSite clone()
  {
    return new AbraSiteLatch(this);
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalLatch(this);
  }

  @Override
  public boolean isIdentical(final AbraBaseSite rhs)
  {
    return false;
  }

  @Override
  public void markReferences()
  {
    super.markReferences();

    if (latchSite != null)
    {
      latchSite.references++;
    }
  }
}
