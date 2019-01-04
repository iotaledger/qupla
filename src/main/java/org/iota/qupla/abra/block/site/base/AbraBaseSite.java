package org.iota.qupla.abra.block.site.base;

import org.iota.qupla.abra.context.AbraPrintContext;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseSite
{
  private static AbraPrintContext printer = new AbraPrintContext();

  public int index;
  public boolean isLatch;
  public String name;
  public AbraBaseSite nullifyFalse;
  public AbraBaseSite nullifyTrue;
  public BaseExpr origin;
  public int references;
  public int size;
  public BaseExpr stmt;
  public String varName; //TODO should be able to remove this

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

  @Override
  public String toString()
  {
    final String oldString = printer.string;
    printer.string = new String(new char[0]);
    eval(printer);
    final String ret = printer.string;
    printer.string = oldString;
    return ret;
  }
}
