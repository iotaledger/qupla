package org.iota.qupla.expression.base;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.parser.Tokenizer;

public abstract class BaseSubExpr extends BaseExpr
{
  public BaseExpr expr;

  public BaseSubExpr(final BaseSubExpr copy)
  {
    super(copy);

    expr = clone(copy.expr);
  }

  public BaseSubExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);
  }

  @Override
  public void analyze()
  {
    expr.analyze();
    size = expr.size;
    typeInfo = expr.typeInfo;
  }

  @Override
  public BaseExpr append()
  {
    return append("(").append(expr).append(")");
  }

  @Override
  public void eval(final CodeContext context)
  {
    expr.eval(context);
  }

}
