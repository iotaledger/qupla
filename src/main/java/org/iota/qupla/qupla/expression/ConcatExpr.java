package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConcatExpr extends BaseBinaryExpr
{
  public ConcatExpr(final ConcatExpr copy)
  {
    super(copy);
  }

  public ConcatExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    BaseExpr leaf = new PostfixExpr(tokenizer).optimize();
    while (tokenizer.tokenId() == Token.TOK_CONCAT)
    {
      final ConcatExpr branch = new ConcatExpr(this);
      leaf = connectBranch(tokenizer, leaf, branch);
      branch.rhs = new PostfixExpr(tokenizer).optimize();
    }

    lhs = leaf;
  }

  @Override
  public void analyze()
  {
    if (rhs != null)
    {
      constTypeInfo = null;
    }

    lhs.analyze();
    size = lhs.size;

    if (rhs != null)
    {
      rhs.analyze();
      size += rhs.size;
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new ConcatExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalConcat(this);
  }
}
