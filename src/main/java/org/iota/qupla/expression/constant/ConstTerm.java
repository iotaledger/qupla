package org.iota.qupla.expression.constant;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.base.BinaryExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class ConstTerm extends BinaryExpr
{
  public ConstTerm(final ConstTerm copy)
  {
    super(copy);
  }

  public ConstTerm(final Tokenizer tokenizer)
  {
    super(tokenizer);

    BaseExpr leaf = new ConstFactor(tokenizer).optimize();
    while (tokenizer.tokenId() == Token.TOK_MUL || tokenizer.tokenId() == Token.TOK_DIV || tokenizer.tokenId() == Token.TOK_MOD)
    {
      final ConstTerm branch = new ConstTerm(this);
      leaf = connectBranch(tokenizer, leaf, branch);
      branch.rhs = new ConstFactor(tokenizer).optimize();
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
      case Token.TOK_MUL:
        size *= rhs.size;
        break;

      case Token.TOK_DIV:
        if (rhs.size == 0)
        {
          rhs.error("Divide by zero in constant expression");
        }

        size /= rhs.size;
        break;

      case Token.TOK_MOD:
        if (rhs.size == 0)
        {
          rhs.error("Divide by zero in constant expression");
        }

        size %= rhs.size;
        break;
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstTerm(this);
  }
}
