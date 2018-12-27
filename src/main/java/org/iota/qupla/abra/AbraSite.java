package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;

public class AbraSite
{
  public int index;
  public boolean isLatch;
  public String name;
  public AbraSite nullifyFalse;
  public AbraSite nullifyTrue;
  public BaseExpr origin;
  public int references;
  public int size;
  public BaseExpr stmt;
  public String type;

  public CodeContext append(final CodeContext context)
  {
    if (stmt != null)
    {
      context.newline().append("" + stmt).newline();
    }

    context.append("// " + index + " ");
    context.append(nullifyTrue != null ? "T" + nullifyTrue.index : nullifyFalse != null ? "F" + nullifyFalse.index : " ");
    return context.append(" " + references + " " + type + " site: ");
  }

  public void code(final TritCode tritCode)
  {
  }

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
