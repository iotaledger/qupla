package org.iota.qupla.qupla.expression.base;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.parser.Tokenizer;

public abstract class BaseSubExpr extends BaseExpr
{
  public BaseExpr expr;

  protected BaseSubExpr(final BaseSubExpr copy)
  {
    super(copy);

    expr = clone(copy.expr);
  }

  protected BaseSubExpr(final Tokenizer tokenizer)
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
  public void eval(final QuplaBaseContext context)
  {
    context.evalSubExpr(this);
  }
}
