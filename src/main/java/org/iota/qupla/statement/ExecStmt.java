package org.iota.qupla.statement;

import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.IntegerExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class ExecStmt extends BaseExpr
{
  public IntegerExpr expected;
  public BaseExpr expr;

  private ExecStmt(final ExecStmt copy)
  {
    super(copy);
    expected = copy.expected;
    expr = copy.expr;
  }

  public ExecStmt(final Tokenizer tokenizer, final boolean test)
  {
    tokenizer.nextToken();
    this.module = tokenizer.module;

    if (test)
    {
      expected = new IntegerExpr(tokenizer);
      expected.expect(tokenizer, Token.TOK_EQUAL, "'='");
    }

    final Token funcName = expect(tokenizer, Token.TOK_NAME, "func name");
    expr = new FuncExpr(tokenizer, funcName);
  }

  @Override
  public void analyze()
  {
    // make sure we always start at call index zero,
    // or else state variables won't work correctly
    final int saveCallNr = callNr;
    callNr = 0;

    expr.analyze();
    typeInfo = expr.typeInfo;

    if (expected != null)
    {
      // make sure expected and expr are same type (integer/float)
      constTypeInfo = expr.typeInfo;
      expected.analyze();
      constTypeInfo = null;
    }

    callNr = saveCallNr;
  }

  @Override
  public BaseExpr append()
  {
    if (expected == null)
    {
      return append("eval ").append(expr);
    }

    return append("test ").append(expected).append(" = ").append(expr);
  }

  @Override
  public BaseExpr clone()
  {
    return new ExecStmt(this);
  }

  public boolean succeed(final TritVector result)
  {
    if (expected.vector.equals(result))
    {
      return true;
    }

    if (!typeInfo.isFloat || expected.vector.size() != result.size())
    {
      return false;
    }

    // when it's a float allow least significant bit to differ
    int size = result.size() - 1;
    return expected.vector.slice(1, size).equals(result.slice(1, size));
  }
}
