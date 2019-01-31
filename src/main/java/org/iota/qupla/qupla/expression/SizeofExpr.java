package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class SizeofExpr extends VectorExpr
{
  public BaseExpr constTypeName;

  private SizeofExpr(final SizeofExpr copy)
  {
    super(copy);

    constTypeName = clone(copy.constTypeName);
  }

  public SizeofExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_SIZEOF, "sizeof");

    constTypeName = new ConstTypeName(tokenizer);
  }

  @Override
  public void analyze()
  {
    constTypeName.analyze();
    name = "" + constTypeName.size;

    super.analyze();
  }

  @Override
  public BaseExpr clone()
  {
    return new SizeofExpr(this);
  }
}
