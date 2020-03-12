package org.iota.qupla.abra.block.base;

import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseBlock
{
  public boolean analyzed;
  public int index;
  public String name;
  public BaseExpr origin;

  public boolean couldBeLutWrapper()
  {
    return false;
  }

  protected void error(final String text)
  {
    throw new CodeException(text);
  }

  public abstract void eval(final AbraBaseContext context);

  public void markReferences()
  {
  }

  public int size()
  {
    return 1;
  }
}
