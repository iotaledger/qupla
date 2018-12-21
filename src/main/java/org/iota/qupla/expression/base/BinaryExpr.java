package org.iota.qupla.expression.base;

import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public abstract class BinaryExpr extends BaseExpr
{
  public BaseExpr lhs;
  public Token operator;
  public BaseExpr rhs;

  protected BinaryExpr(final BinaryExpr copy)
  {
    super(copy);

    lhs = clone(copy.lhs);
    operator = copy.operator;
    rhs = clone(copy.rhs);
  }

  protected BinaryExpr(final Tokenizer tokenizer)
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

  protected BaseExpr connectBranch(final Tokenizer tokenizer, BaseExpr leaf, final BinaryExpr branch)
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
