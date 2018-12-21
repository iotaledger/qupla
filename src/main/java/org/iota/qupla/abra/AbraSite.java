package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;

public class AbraSite
{
  public int index;
  public String name;
  public BaseExpr origin;
  public int size;
  public BaseExpr stmt;
  public String type;

  public CodeContext append(final CodeContext context)
  {
    if (stmt != null)
    {
      context.newline().append("" + stmt).newline();
    }
    return context.append("// " + type + " site " + index + ": ");
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

  public int refer(final int site)
  {
    //    if (site < index)
    //    {
    //      return index - 1 - site;
    //    }

    return site;
  }
}
