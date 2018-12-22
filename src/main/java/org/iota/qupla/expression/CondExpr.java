package org.iota.qupla.expression;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;
import org.iota.qupla.statement.TypeStmt;

public class CondExpr extends BaseExpr
{
  public BaseExpr condition;
  public BaseExpr falseBranch;
  public BaseExpr trueBranch;

  public CondExpr(final CondExpr copy)
  {
    super(copy);

    condition = clone(copy.condition);
    trueBranch = clone(copy.trueBranch);
    falseBranch = clone(copy.falseBranch);
  }

  public CondExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    condition = new MergeExpr(tokenizer).optimize();

    if (tokenizer.tokenId() == Token.TOK_QUESTION)
    {
      tokenizer.nextToken();

      trueBranch = new MergeExpr(tokenizer).optimize();

      expect(tokenizer, Token.TOK_COLON, "':'");

      if (tokenizer.tokenId() == Token.TOK_NULL)
      {
        tokenizer.nextToken();
        return;
      }

      falseBranch = new CondExpr(tokenizer).optimize();
    }
  }

  @Override
  public void analyze()
  {
    if (trueBranch == null)
    {
      // should have been optimized away
      condition.analyze();
      size = condition.size;
      typeInfo = condition.typeInfo;
      return;
    }

    final TypeStmt saved = constTypeInfo;
    constTypeInfo = null;
    condition.analyze();

    if (condition.size != 1)
    {
      condition.error("Condition should be single trit");
    }

    constTypeInfo = saved;
    trueBranch.analyze();

    if (falseBranch != null)
    {
      constTypeInfo = saved;
      falseBranch.analyze();

      if (trueBranch.size != falseBranch.size)
      {
        falseBranch.error("Conditional branches size mismatch");
      }
    }

    size = trueBranch.size;
    typeInfo = trueBranch.typeInfo;
  }

  @Override
  public BaseExpr append()
  {
    append(condition);

    if (trueBranch != null)
    {
      append(" ? ").append(trueBranch).append(" : ");
      if (falseBranch == null)
      {
        return append("null");
      }

      append(falseBranch);
    }

    return this;
  }

  @Override
  public BaseExpr clone()
  {
    return new CondExpr(this);
  }

  @Override
  public void eval(final CodeContext context)
  {
    context.evalConditional(this);
  }

  @Override
  public BaseExpr optimize()
  {
    if (trueBranch == null)
    {
      return condition;
    }

    return super.optimize();
  }
}
