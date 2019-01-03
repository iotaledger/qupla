package org.iota.qupla.qupla.expression.constant;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstTypeName extends BaseExpr
{
  public ConstTypeName(final ConstTypeName copy)
  {
    super(copy);

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
          return;
        }
      }
    }
  }

  public ConstTypeName(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token typeName = expect(tokenizer, Token.TOK_NAME, "type name");
    name = typeName.text;
  }

  @Override
  public void analyze()
  {
    typeInfo = analyzeType();
    name = typeInfo.name;
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstTypeName(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalBaseExpr(this);
  }
}
