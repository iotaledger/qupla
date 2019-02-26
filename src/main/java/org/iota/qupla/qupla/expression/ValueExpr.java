package org.iota.qupla.qupla.expression;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ValueExpr extends VectorExpr
{
  private ValueExpr(final ValueExpr copy)
  {
    super(copy);
  }

  public ValueExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    switch (tokenizer.tokenId())
    {
    case Token.TOK_FALSE:
      name = "" + TritConverter.BOOL_FALSE;
      tokenizer.nextToken();
      return;

    case Token.TOK_TRUE:
      name = "" + TritConverter.BOOL_TRUE;
      tokenizer.nextToken();
      return;

    case Token.TOK_MINUS:
      name = "-";
      tokenizer.nextToken();
      break;
    }

    if (tokenizer.tokenId() == Token.TOK_NUMBER || tokenizer.tokenId() == Token.TOK_FLOAT)
    {
      name += tokenizer.currentToken().text;
      tokenizer.nextToken();
    }
  }

  @Override
  public void analyze()
  {
    removeLeadingZeroes();

    super.analyze();
  }

  @Override
  public BaseExpr clone()
  {
    return new ValueExpr(this);
  }

  private void removeLeadingZeroes()
  {
    if (name.equals("-"))
    {
      // just a single trit
      return;
    }

    // strip and save starting minus sign
    final boolean negative = name.startsWith("-");
    if (negative)
    {
      name = name.substring(1);
    }

    // strip leading zeroes
    while (name.startsWith("0"))
    {
      name = name.substring(1);
    }

    // decimal point?
    final int dot = name.indexOf('.');
    if (dot >= 0)
    {
      // strip trailing zeroes
      while (name.endsWith("0"))
      {
        name = name.substring(0, name.length() - 1);
      }

      // strip trailing dot
      if (name.endsWith("."))
      {
        name = name.substring(0, name.length() - 1);
      }
    }

    // re-insert at least one leading zero?
    if (name.length() == 0 || name.startsWith("."))
    {
      name = "0" + name;
    }

    // restore minus sign?
    if (negative && !name.equals("0"))
    {
      name = "-" + name;
    }
  }
}
