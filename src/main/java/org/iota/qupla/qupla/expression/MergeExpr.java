package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class MergeExpr extends BaseBinaryExpr
{
  public MergeExpr(final MergeExpr copy)
  {
    super(copy);
  }

  public MergeExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    BaseExpr leaf = new ConcatExpr(tokenizer).optimize();
    while (tokenizer.tokenId() == Token.TOK_MERGE)
    {
      final MergeExpr branch = new MergeExpr(this);
      leaf = connectBranch(tokenizer, leaf, branch);
      branch.rhs = new ConcatExpr(tokenizer).optimize();
    }

    lhs = leaf;
  }

  @Override
  public void analyze()
  {
    lhs.analyze();
    size = lhs.size;

    if (rhs != null)
    {
      rhs.analyze();
      if (rhs.size != size)
      {
        rhs.error("Invalid merge input size");
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new MergeExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalMerge(this);
  }
}
