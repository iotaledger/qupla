package org.iota.qupla.qupla.expression;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
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
    case Token.TOK_LITERAL_BITS:
    case Token.TOK_LITERAL_FLOAT:
    case Token.TOK_LITERAL_HEX:
    case Token.TOK_LITERAL_NUMBER:
    case Token.TOK_LITERAL_TRITS:
    case Token.TOK_LITERAL_TRYTES:
      name = tokenizer.currentToken().text;
      tokenizer.nextToken();
      return;

    case Token.TOK_LITERAL_FALSE:
      name = TritConverter.BOOL_FALSE;
      tokenizer.nextToken();
      return;

    case Token.TOK_LITERAL_TRUE:
      name = TritConverter.BOOL_TRUE;
      tokenizer.nextToken();
      return;

    case Token.TOK_MINUS:
      name = "-";
      tokenizer.nextToken();
      break;
    }

    if (tokenizer.tokenId() == Token.TOK_LITERAL_NUMBER || tokenizer.tokenId() == Token.TOK_LITERAL_FLOAT)
    {
      name += tokenizer.currentToken().text;
      tokenizer.nextToken();
    }
  }

  @Override
  public void analyze()
  {
    switch (origin.id)
    {
    case Token.TOK_LITERAL_FALSE:
    case Token.TOK_LITERAL_FLOAT:
    case Token.TOK_LITERAL_NUMBER:
    case Token.TOK_LITERAL_TRUE:
    case Token.TOK_MINUS:
      removeLeadingZeroes();
      super.analyze();
      return;

    case Token.TOK_LITERAL_BITS:
      vector = new TritVector(TritConverter.fromLong(Long.valueOf(name.substring(2), 2)));
      break;

    case Token.TOK_LITERAL_HEX:
      vector = new TritVector(TritConverter.fromLong(Long.valueOf(name.substring(2), 16)));
      break;

    case Token.TOK_LITERAL_TRITS:
      vector = new TritVector(name.substring(2));
      break;

    case Token.TOK_LITERAL_TRYTES:
      vector = TritVector.fromTrytes(name.substring(2));
      break;
    }

    size = vector.size();
    if (constTypeInfo != null)
    {
      size = constTypeInfo.size;
      if (vector.size() < size)
      {
        vector = TritVector.concat(vector, new TritVector(size - vector.size(), TritVector.TRIT_ZERO));
      }
    }
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
