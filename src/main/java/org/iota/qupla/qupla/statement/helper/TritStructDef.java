package org.iota.qupla.qupla.statement.helper;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class TritStructDef extends BaseExpr
{
  public final ArrayList<BaseExpr> fields = new ArrayList<>();

  public TritStructDef(final TritStructDef copy)
  {
    super(copy);

    cloneArray(fields, copy.fields);
  }

  public TritStructDef(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'");

    do
    {
      final ConstTypeName fieldType = new ConstTypeName(tokenizer);

      final NameExpr field = new NameExpr(tokenizer, "field name");
      field.type = fieldType;

      fields.add(field);
    }
    while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE);

    tokenizer.nextToken();
  }

  @Override
  public void analyze()
  {
    for (final BaseExpr field : fields)
    {
      field.analyze();
      size += field.size;
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new TritStructDef(this);
  }
}
