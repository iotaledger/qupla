package org.iota.qupla.qupla.expression.constant;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstNumber extends BaseExpr
{
  private ConstNumber(final ConstNumber copy)
  {
    super(copy);
  }

  public ConstNumber(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token number = expect(tokenizer, Token.TOK_NUMBER, "number");
    name = number.text;
  }

  @Override
  public void analyze()
  {
    size = Integer.parseInt(name);
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstNumber(this);
  }
}
