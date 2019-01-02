package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class NameExpr extends BaseExpr
{
  public BaseExpr type;

  public NameExpr(final NameExpr copy)
  {
    super(copy);

    type = clone(copy.type);
  }

  public NameExpr(final Tokenizer tokenizer, final String what)
  {
    super(tokenizer);

    final Token varName = expect(tokenizer, Token.TOK_NAME, what);
    name = varName.text;
  }

  @Override
  public void analyze()
  {
    if (type != null)
    {
      type.analyze();
      typeInfo = type.typeInfo;
      size = type.size;
    }
  }

  @Override
  public BaseExpr append()
  {
    if (type != null)
    {
      append(type).append(" ");
    }

    return append(name);
  }

  @Override
  public BaseExpr clone()
  {
    return new NameExpr(this);
  }
}
