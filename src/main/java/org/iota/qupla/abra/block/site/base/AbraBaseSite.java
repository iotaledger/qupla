package org.iota.qupla.abra.block.site.base;

import org.iota.qupla.abra.context.AbraPrintContext;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseSite
{
  public static final AbraPrintContext printer = new AbraPrintContext();

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

  protected AbraBaseSite()
  {
  }

  protected AbraBaseSite(final AbraBaseSite copy)
  {
    index = copy.index;
    isLatch = copy.isLatch;
    size = copy.size;
  }

  public abstract AbraBaseSite clone();

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

  public boolean isIdentical(final AbraBaseSite rhs)
  {
    if (getClass() != rhs.getClass())
    {
      return false;
    }

    if (size != rhs.size || isLatch != rhs.isLatch)
    {
      return false;
    }

    return true;
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
