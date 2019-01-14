package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class FieldExpr extends BaseSubExpr
{
  private FieldExpr(final FieldExpr copy)
  {
    super(copy);
  }

  public FieldExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token fieldName = expect(tokenizer, Token.TOK_NAME, "field name");
    name = fieldName.text;

    expect(tokenizer, Token.TOK_EQUAL, "'='");

    expr = new CondExpr(tokenizer).optimize();
  }

  @Override
  public BaseExpr clone()
  {
    return new FieldExpr(this);
  }
}
