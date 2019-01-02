package org.iota.qupla.abra.block.site.base;

import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseSite
{
  public int index;
  public boolean isLatch;
  public String name;
  public AbraBaseSite nullifyFalse;
  public AbraBaseSite nullifyTrue;
  public int oldSize;
  public BaseExpr origin;
  public int references;
  public int size;
  public BaseExpr stmt;
  public String type;
  public String varName;

  public abstract void eval(final AbraBaseContext context);

  public void from(final BaseExpr expr)
  {
    origin = expr;
    name = expr.name;
    size = expr.size;
  }

  public boolean hasNullifier()
  {
    return nullifyFalse != null || nullifyTrue != null;
  }

  public void markReferences()
  {
    if (nullifyFalse != null)
    {
      nullifyFalse.references++;
    }

    if (nullifyTrue != null)
    {
      nullifyTrue.references++;
    }
  }

  public int refer(final int site)
  {
    //    if (site < index)
    //    {
    //      return index - 1 - site;
    //    }

    return site;
  }
}
