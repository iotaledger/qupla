package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class SubExpr extends BaseSubExpr
{
  public SubExpr(final SubExpr copy)
  {
    super(copy);
  }

  public SubExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_FUNC_OPEN, "'('");

    expr = new CondExpr(tokenizer).optimize();

    expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'");
  }

  @Override
  public BaseExpr clone()
  {
    return new SubExpr(this);
  }
}
