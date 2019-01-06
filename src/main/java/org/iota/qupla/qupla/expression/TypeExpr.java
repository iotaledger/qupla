package org.iota.qupla.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class TypeExpr extends BaseExpr
{
  public final ArrayList<BaseExpr> fields = new ArrayList<>();
  public BaseExpr type;

  public TypeExpr(final TypeExpr copy)
  {
    super(copy);

    cloneArray(fields, copy.fields);
    type = clone(copy.type);
  }

  public TypeExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    type = new ConstTypeName(tokenizer, identifier);

    expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'");

    do
    {
      fields.add(new FieldExpr(tokenizer));
    }
    while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE);

    tokenizer.nextToken();
  }

  @Override
  public void analyze()
  {
    type.analyze();
    typeInfo = type.typeInfo;
    size = typeInfo.size;
    name = typeInfo.name;
    if (typeInfo.struct == null)
    {
      error("Expected structured trit vector type name");
    }

    for (final BaseExpr field : fields)
    {
      field.analyze();

      // check that this is a field name
      boolean found = false;
      for (final BaseExpr structField : typeInfo.struct.fields)
      {
        if (field.name.equals(structField.name))
        {
          found = true;
          break;
        }
      }

      if (!found)
      {
        field.error("Unknown field name: " + field.name);
      }
    }

    // check that all subfields of the struct vector are assigned
    // also check the assigned size
    // also sort the fields in the same order as in the struct vector
    final ArrayList<BaseExpr> sortedFields = new ArrayList<>();
    for (final BaseExpr structField : typeInfo.struct.fields)
    {
      boolean found = false;
      for (final BaseExpr field : fields)
      {
        if (field.name.equals(structField.name))
        {
          found = true;
          if (field.size != structField.size)
          {
            field.error("Structured trit field size mismatch: " + field.name);
          }

          for (final BaseExpr sortedField : sortedFields)
          {
            if (field.name.equals(sortedField.name))
            {
              field.error("Duplicate field name: " + field.name);
            }
          }

          sortedFields.add(field);
        }
      }

      if (!found)
      {
        error("Missing assignment to field: " + structField.name);
      }
    }

    fields.clear();
    fields.addAll(sortedFields);
  }

  @Override
  public BaseExpr clone()
  {
    return new TypeExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalType(this);
  }
}
