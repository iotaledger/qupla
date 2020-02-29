package org.iota.qupla.qupla.statement;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.helper.TritStructDef;
import org.iota.qupla.qupla.statement.helper.TritVectorDef;

public class TypeStmt extends BaseExpr
{
  public boolean isFloat;
  public TritStructDef struct;
  public TritVectorDef vector;

  public TypeStmt(final TypeStmt copy)
  {
    super(copy);

    isFloat = copy.isFloat;
    struct = copy.struct == null ? null : new TritStructDef(copy.struct);
    vector = copy.vector == null ? null : new TritVectorDef(copy.vector);
  }

  public TypeStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_TYPE, "type");

    final Token typeName = expect(tokenizer, Token.TOK_NAME, "type name");
    name = typeName.text;
    module.checkDuplicateName(module.types, this);

    if (tokenizer.tokenId() == Token.TOK_GROUP_OPEN)
    {
      struct = new TritStructDef(tokenizer, typeName);
      return;
    }

    vector = new TritVectorDef(tokenizer, typeName);
  }

  @Override
  public void analyze()
  {
    if (size > 0)
    {
      return;
    }

    if (struct != null)
    {
      struct.analyze();
      size = struct.size;

      if (struct.fields.size() == 2)
      {
        final BaseExpr mantissa = struct.fields.get(0);
        if (mantissa.name.equals("mantissa"))
        {
          final BaseExpr exponent = struct.fields.get(1);
          isFloat = exponent.name.equals("exponent");
        }
      }

      return;
    }

    vector.analyze();
    size = vector.size;
  }

  @Override
  public BaseExpr clone()
  {
    return new TypeStmt(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalTypeDefinition(this);
  }

  public String toString(final TritVector value)
  {
    if (value.isNull())
    {
      return "NULL";
    }

    if (!value.isValue())
    {
      return "***SOME NULL TRITS***";
    }

    if (isFloat)
    {
      final BaseExpr mantissa = struct.fields.get(0);
      final BaseExpr exponent = struct.fields.get(1);
      return TritConverter.toFloat(value.trits(), mantissa.size, exponent.size);
    }

    if (struct == null)
    {
      if (size > 81)
      {
        return new String(value.trits());
      }

      return value.toDecimal();
    }

    String result = "{ ";
    int offset = 0;
    boolean first = true;
    for (final BaseExpr field : struct.fields)
    {
      result += first ? "" : ", ";
      first = false;
      final TritVector slice = value.slicePadded(offset, field.size);
      result += field.name + " = " + slice.toDecimal();
      offset += field.size;
    }

    return result + " }";
  }

  @Override
  public String toString()
  {
    if (vector != null)
    {
      return super.toString();
    }

    return "type " + name + " { ... }";
  }
}
