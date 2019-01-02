package org.iota.qupla.qupla.expression.constant;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstSubExpr extends BaseSubExpr
{
  public ConstSubExpr(final ConstSubExpr copy)
  {
    super(copy);
  }

  public ConstSubExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expr = new ConstExpr(tokenizer).optimize();

    expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'");
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstSubExpr(this);
  }
}
