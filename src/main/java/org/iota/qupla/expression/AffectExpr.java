package org.iota.qupla.expression;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.constant.ConstNumber;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class AffectExpr extends BaseExpr
{
  public BaseExpr delay;

  public AffectExpr(final AffectExpr copy)
  {
    super(copy);

    delay = clone(copy.delay);
  }

  public AffectExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_AFFECT, "affect");

    final Token varName = expect(tokenizer, Token.TOK_NAME, "environment name");
    name = varName.text;

    if (tokenizer.tokenId() == Token.TOK_DELAY)
    {
      tokenizer.nextToken();
      delay = new ConstNumber(tokenizer);
    }
  }

  @Override
  public void analyze()
  {
    if (delay != null)
    {
      delay.analyze();
    }
  }

  @Override
  public BaseExpr append()
  {
    append("affect ").append(name);
    if (delay != null)
    {
      append(" delay ").append(delay);
    }

    return this;
  }

  @Override
  public BaseExpr clone()
  {
    return new AffectExpr(this);
  }
}
