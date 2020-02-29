package org.iota.qupla.qupla.statement.helper;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class LutEntry extends BaseExpr
{
  public final String inputs;
  public final String outputs;

  private LutEntry(final LutEntry copy)
  {
    super(copy);

    inputs = copy.inputs;
    outputs = copy.outputs;
  }

  public LutEntry(final Tokenizer tokenizer)
  {
    super(tokenizer);

    inputs = parseTritList(tokenizer);
    expect(tokenizer, Token.TOK_EQUAL, "'='");
    outputs = parseTritList(tokenizer);
  }

  @Override
  public void analyze()
  {
  }

  @Override
  public BaseExpr clone()
  {
    return new LutEntry(this);
  }

  private String parseTrit(final Tokenizer tokenizer)
  {
    final Token trit = tokenizer.currentToken();
    switch (trit.id)
    {
    case Token.TOK_LITERAL_FALSE:
      tokenizer.nextToken();
      return TritConverter.BOOL_FALSE;

    case Token.TOK_LITERAL_TRUE:
      tokenizer.nextToken();
      return TritConverter.BOOL_TRUE;

    case Token.TOK_MINUS:
      tokenizer.nextToken();
      return trit.text;
    }

    expect(tokenizer, Token.TOK_LITERAL_NUMBER, "trit value, 'false', or 'true'");
    if (trit.text.length() != 1 || trit.text.charAt(0) > '1')
    {
      error(trit, "Invalid trit value");
    }

    return trit.text;
  }

  private String parseTritList(final Tokenizer tokenizer)
  {
    String trits = parseTrit(tokenizer);
    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();
      trits += parseTrit(tokenizer);
    }

    return trits;
  }
}
