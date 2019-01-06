package org.iota.qupla.qupla.expression.constant;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.TypeStmt;

public class ConstTypeName extends BaseExpr
{
  public ConstTypeName(final ConstTypeName copy)
  {
    super(copy);
  }

  public ConstTypeName(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token typeName = expect(tokenizer, Token.TOK_NAME, "type name");
    name = typeName.text;
  }

  public ConstTypeName(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);
  }

  @Override
  public void analyze()
  {
    if (currentUse != null)
    {
      // while cloning function replace placeholder type names
      // with the actual type names as defined in the use statement
      final ArrayList<BaseExpr> typeArgs = currentUse.typeInstantiations.get(currentUseIndex);
      for (int i = 0; i < currentUse.template.params.size(); i++)
      {
        final BaseExpr param = currentUse.template.params.get(i);
        if (name.equals(param.name))
        {
          final BaseExpr typeName = typeArgs.get(i);
          name = typeName.name;
          break;
        }
      }

      for (final BaseExpr type : currentUse.template.types)
      {
        if (type.name.equals(name))
        {
          if (type.typeInfo.size == 0)
          {
            error("Did not analyze: " + name);
          }

          typeInfo = type.typeInfo;
          if (typeInfo.vector != null && typeInfo.vector.typeInfo != null)
          {
            typeInfo = typeInfo.vector.typeInfo;
            size = typeInfo.size;
            name = typeInfo.name;
            return;
          }

          if (typeInfo.vector != null)
          {
            // see if we can find a matching type in the module type list
            for (final TypeStmt existing : module.types)
            {
              if (existing.size == typeInfo.size && existing.vector != null)
              {
                typeInfo.vector.typeInfo = existing;
                typeInfo = existing;
                break;
              }
            }
          }

          size = typeInfo.size;
          name = typeInfo.name;

          return;
        }
      }
    }

    typeInfo = (TypeStmt) findEntity(TypeStmt.class, "type");
    size = typeInfo.size;
    name = typeInfo.name;
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstTypeName(this);
  }
}
