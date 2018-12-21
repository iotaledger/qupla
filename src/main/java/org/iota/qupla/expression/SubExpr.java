package org.iota.qupla.expression;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class SubExpr extends BaseSubExpr
{
  public SubExpr(final SubExpr copy)
  {
    super(copy);
  }

  public SubExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expr = new MergeExpr(tokenizer).optimize();

    expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'");
  }

  @Override
  public BaseExpr clone()
  {
    return new SubExpr(this);
  }
}
