package org.iota.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.base.BinaryExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class ConcatExpr extends BinaryExpr
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
  public void eval(final CodeContext context)
  {
    final ArrayList<BaseExpr> exprs = new ArrayList<>();
    exprs.add(lhs);
    if (rhs != null)
    {
      exprs.add(rhs);
    }

    context.evalConcat(exprs);
  }
}
