package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class AssignExpr extends BaseExpr
{
  private static final boolean useBreak = false;

  public BaseExpr expr;
  public int stateIndex;

  public AssignExpr(final AssignExpr copy)
  {
    super(copy);

    expr = clone(copy.expr);
    stateIndex = copy.stateIndex;
  }

  public AssignExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    final Token varName = expect(tokenizer, Token.TOK_NAME, "variable name");
    name = varName.text;

    for (int i = scope.size() - 1; i >= 0; i--)
    {
      final BaseExpr var = scope.get(i);
      if (var.name.equals(name))
      {
        if (var instanceof StateExpr)
        {
          stateIndex = i;
          break;
        }

        error("Duplicate variable name: " + name);
      }
    }

    expect(tokenizer, Token.TOK_EQUAL, "'='");

    expr = new CondExpr(tokenizer).optimize();
    stackIndex = scope.size();
    scope.add(this);

    if (useBreak && expr.name != null && expr.name.equals("break"))
    {
      int breakPoint = 0;
    }
  }

  @Override
  public void analyze()
  {
    constTypeInfo = null;
    if (stateIndex != 0)
    {
      final BaseExpr stateVar = scope.get(stateIndex);
      constTypeInfo = stateVar.typeInfo;
    }

    expr.analyze();
    if (size != 0 && size != expr.size)
    {
      expr.error("Expression size mismatch");
    }

    size = expr.size;
    typeInfo = expr.typeInfo;

    if (stateIndex != 0)
    {
      final BaseExpr stateVar = scope.get(stateIndex);
      if (stateVar.size != size)
      {
        expr.error("State variable size mismatch");
      }
    }

    scope.add(this);
  }

  @Override
  public BaseExpr append()
  {
    return append(name).append(" = ").append(expr);
  }

  @Override
  public BaseExpr clone()
  {
    return new AssignExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalAssign(this);
  }
}
