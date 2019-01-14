package org.iota.qupla.qupla.expression.constant;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.TypeStmt;

public class ConstTypeName extends BaseExpr
{
  private ConstTypeName(final ConstTypeName copy)
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
      for (int i = 0; i < currentUse.template.params.size(); i++)
      {
        final BaseExpr param = currentUse.template.params.get(i);
        if (name.equals(param.name))
        {
          final BaseExpr typeName = currentUse.typeArgs.get(i);
          typeInfo = typeName.typeInfo;
          size = typeInfo.size;
          name = typeInfo.name;
          return;
        }
      }

      for (final BaseExpr type : currentUse.types)
      {
        if (type.name.equals(name))
        {
          if (type.size == 0)
          {
            error("Did not analyze: " + name);
          }

          typeInfo = (TypeStmt) type;
          if (typeInfo.vector != null)
          {
            if (typeInfo.vector.typeInfo != null)
            {
              typeInfo = typeInfo.vector.typeInfo;
              size = typeInfo.size;
              name = typeInfo.name;
              return;
            }

            // see if we can find a matching basic type in the module type list
            for (final TypeStmt existing : module.types)
            {
              if (existing.size == typeInfo.size)
              {
                if (existing.vector != null && existing.vector.typeExpr instanceof ConstNumber)
                {
                  typeInfo.vector.typeInfo = existing;
                  typeInfo = existing;
                  size = typeInfo.size;
                  name = typeInfo.name;
                  return;
                }
              }
            }

            // create a new basic type in the module type list
            for (final TypeStmt existing : module.types)
            {
              if (existing.vector != null && existing.vector.typeExpr instanceof ConstNumber)
              {
                final TypeStmt newType = new TypeStmt(existing);
                newType.size = typeInfo.size;
                newType.name = "Trit__" + newType.size;
                newType.vector.size = newType.size;
                newType.vector.name = newType.name;
                newType.vector.typeExpr.size = newType.size;
                newType.vector.typeExpr.name = "" + newType.size;
                module.types.add(newType);

                typeInfo.vector.typeInfo = newType;
                typeInfo = newType;
                size = typeInfo.size;
                name = typeInfo.name;
                return;
              }
            }
          }

          size = typeInfo.size;
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
