package org.iota.qupla.expression;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class FieldExpr extends BaseSubExpr
{
  public FieldExpr(final FieldExpr copy)
  {
    super(copy);
  }

  public FieldExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token fieldName = expect(tokenizer, Token.TOK_NAME, "field name");
    name = fieldName.text;

    expect(tokenizer, Token.TOK_EQUAL, "'='");

    expr = new MergeExpr(tokenizer).optimize();
  }

  @Override
  public BaseExpr append()
  {
    return append(name).append(" = ").append(expr);
  }

  @Override
  public BaseExpr clone()
  {
    return new FieldExpr(this);
  }
}
