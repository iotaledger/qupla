package org.iota.qupla.qupla.expression.constant;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstFactor extends BaseSubExpr
{
  public final ArrayList<BaseExpr> fields = new ArrayList<>();
  public boolean negative;

  private ConstFactor(final ConstFactor copy)
  {
    super(copy);

    fields.addAll(copy.fields);
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

    case Token.TOK_MINUS:
      negative = true;
      expr = new ConstFactor(tokenizer);
      return;

    case Token.TOK_NUMBER:
      expr = new ConstNumber(tokenizer);
      return;
    }

    final Token varName = expect(tokenizer, Token.TOK_NAME, "variable name");
    name = varName.text;
    if (tokenizer.tokenId() == Token.TOK_FUNC_OPEN || tokenizer.tokenId() == Token.TOK_TEMPL_OPEN)
    {
      expr = new ConstFuncExpr(tokenizer, varName);
      return;
    }

    expr = new ConstTypeName(tokenizer, varName);
    while (tokenizer.tokenId() == Token.TOK_DOT)
    {
      tokenizer.nextToken();

      fields.add(new NameExpr(tokenizer, "field name"));
    }
  }

  @Override
  public void analyze()
  {
    expr.analyze();
    typeInfo = expr.typeInfo;
    size = negative ? -expr.size : expr.size;

    String fieldPath = name;
    for (final BaseExpr field : fields)
    {
      if (typeInfo == null || typeInfo.struct == null)
      {
        error("Expected structured trit vector: " + fieldPath);
      }

      boolean found = false;
      for (final BaseExpr structField : typeInfo.struct.fields)
      {
        if (structField.name.equals(field.name))
        {
          found = true;
          typeInfo = structField.typeInfo;
          size = structField.size;
          break;
        }
      }

      if (!found)
      {
        error("Invalid structured trit vector field name: " + field);
      }

      fieldPath += "." + field;
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstFactor(this);
  }

  @Override
  public BaseExpr optimize()
  {
    return negative || fields.size() != 0 ? this : expr;
  }
}
