package org.iota.qupla.qupla.expression.base;

import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public abstract class BaseBinaryExpr extends BaseExpr
{
  public BaseExpr lhs;
  public Token operator;
  public BaseExpr rhs;

  protected BaseBinaryExpr(final BaseBinaryExpr copy)
  {
    super(copy);

    lhs = clone(copy.lhs);
    operator = copy.operator;
    rhs = clone(copy.rhs);
  }

  protected BaseBinaryExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);
  }

  @Override
  public BaseExpr append()
  {
    append(lhs);
    if (rhs != null)
    {
      append(" " + operator.text + " ").append(rhs);
    }

    return this;
  }

  protected BaseExpr connectBranch(final Tokenizer tokenizer, BaseExpr leaf, final BaseBinaryExpr branch)
  {
    branch.lhs = leaf;
    branch.origin = leaf.origin;

    branch.operator = tokenizer.currentToken();
    tokenizer.nextToken();

    return branch;
  }

  public BaseExpr optimize()
  {
    return rhs == null ? lhs : this;
  }
}
