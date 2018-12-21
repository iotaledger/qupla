package org.iota.qupla.statement.helper;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class LutEntry extends BaseExpr
{
  public final String inputs;
  public final String outputs;

  public LutEntry(final LutEntry copy)
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
  public BaseExpr append()
  {
    boolean first = true;
    for (int i = 0; i < inputs.length(); i++)
    {
      append(first ? "" : ",").append(inputs.substring(i, i + 1));
      first = false;
    }

    append(" = ");

    first = true;
    for (int i = 0; i < outputs.length(); i++)
    {
      append(first ? "" : ",").append(outputs.substring(i, i + 1));
      first = false;
    }

    return this;
  }

  @Override
  public BaseExpr clone()
  {
    return new LutEntry(this);
  }

  private String parseTrit(final Tokenizer tokenizer)
  {
    final Token trit = tokenizer.currentToken();
    if (trit.id == Token.TOK_MINUS)
    {
      tokenizer.nextToken();
      return trit.text;
    }

    expect(tokenizer, Token.TOK_NUMBER, "trit value");
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
