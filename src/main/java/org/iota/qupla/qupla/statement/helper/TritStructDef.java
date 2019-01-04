package org.iota.qupla.qupla.statement.helper;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.base.BaseExpr;
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

    name = identifier.text;

    tokenizer.nextToken();
    do
    {
      final Token fieldName = expect(tokenizer, Token.TOK_NAME, "field name");

      fields.add(new TritVectorDef(tokenizer, fieldName));
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
