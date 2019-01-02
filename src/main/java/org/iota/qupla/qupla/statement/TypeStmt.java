package org.iota.qupla.qupla.statement;

import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.helper.TritStructDef;
import org.iota.qupla.qupla.statement.helper.TritVectorDef;

public class TypeStmt extends BaseExpr
{
  public boolean fromTritCode;
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
  public BaseExpr append()
  {
    append("type ");
    if (struct != null)
    {
      return append(struct);
    }

    return append(vector);
  }

  @Override
  public BaseExpr clone()
  {
    return new TypeStmt(this);
  }

  public String display(final TritVector value)
  {
    if (!isFloat)
    {
      return value.display(0, 0);
    }

    final BaseExpr mantissa = struct.fields.get(0);
    final BaseExpr exponent = struct.fields.get(1);
    return value.display(mantissa.size, exponent.size);
  }

  public String displayValue(final TritVector value)
  {
    if (!isFloat)
    {
      return value.displayValue(0, 0);
    }

    final BaseExpr mantissa = struct.fields.get(0);
    final BaseExpr exponent = struct.fields.get(1);
    return value.displayValue(mantissa.size, exponent.size);
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
