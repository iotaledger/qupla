package org.iota.qupla.expression.constant;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.base.BinaryExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class ConstExpr extends BinaryExpr
{
  public ConstExpr(final ConstExpr copy)
  {
    super(copy);
  }

  public ConstExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    BaseExpr leaf = new ConstTerm(tokenizer).optimize();
    while (tokenizer.tokenId() == Token.TOK_PLUS || tokenizer.tokenId() == Token.TOK_MINUS)
    {
      final ConstExpr branch = new ConstExpr(this);
      leaf = connectBranch(tokenizer, leaf, branch);
      branch.rhs = new ConstTerm(tokenizer).optimize();
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
      switch (operator.id)
      {
      case Token.TOK_PLUS:
        size += rhs.size;
        break;

      case Token.TOK_MINUS:
        size -= rhs.size;
        break;
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstExpr(this);
  }
}
