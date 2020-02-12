package org.iota.qupla.abra.block.site.base;

import org.iota.qupla.abra.context.AbraPrintContext;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseSite
{
  public static final AbraPrintContext printer = new AbraPrintContext(null);

  public int index;
  public String name;
  public BaseExpr origin;
  public int references;
  public int size;
  public BaseExpr stmt;

  protected AbraBaseSite()
  {
  }

  protected AbraBaseSite(final AbraBaseSite copy)
  {
    index = copy.index;
    size = copy.size;
  }

  public abstract AbraBaseSite clone();

  protected void error(final String text)
  {
    throw new CodeException(text);
  }

  public abstract void eval(final AbraBaseContext context);

  public void from(final BaseExpr expr)
  {
    origin = expr;
    size = expr.size;
  }

  public boolean isIdentical(final AbraBaseSite rhs)
  {
    if (getClass() != rhs.getClass())
    {
      return false;
    }

    return size == rhs.size;
  }

  public void markReferences()
  {
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

  public String varName()
  {
    return name == null ? "p" + index : name;
  }
}
