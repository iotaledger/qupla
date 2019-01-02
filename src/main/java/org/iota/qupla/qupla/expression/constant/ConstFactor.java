package org.iota.qupla.qupla.expression.constant;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstFactor extends BaseSubExpr
{
  public boolean negative;

  public ConstFactor(final ConstFactor copy)
  {
    super(copy);
    negative = copy.negative;
  }

  public ConstFactor(final Tokenizer tokenizer)
  {
    super(tokenizer);

    switch (tokenizer.tokenId())
    {
    case Token.TOK_FUNC_OPEN:
      expr = new ConstSubExpr(tokenizer);
      return;

    case Token.TOK_NAME:
      expr = new ConstTypeName(tokenizer);
      return;

    case Token.TOK_NUMBER:
      expr = new ConstNumber(tokenizer);
      return;
    }

    expect(tokenizer, Token.TOK_MINUS, "name, number, '-', or '('");

    negative = true;
    expr = new ConstFactor(tokenizer);
  }

  @Override
  public void analyze()
  {
    expr.analyze();
    size = negative ? -expr.size : expr.size;
  }

  @Override
  public BaseExpr append()
  {
    return append(negative ? "-" : "").append(expr);
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstFactor(this);
  }

  @Override
  public BaseExpr optimize()
  {
    return negative ? this : expr;
  }
}
