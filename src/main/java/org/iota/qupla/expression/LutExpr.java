package org.iota.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;
import org.iota.qupla.statement.LutStmt;

public class LutExpr extends BaseExpr
{
  public final ArrayList<BaseExpr> args = new ArrayList<>();
  public LutStmt lut;

  public LutExpr(final LutExpr copy)
  {
    super(copy);

    cloneArray(args, copy.args);
    lut = copy.lut;
  }

  public LutExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    name = identifier.text;

    expect(tokenizer, Token.TOK_ARRAY_OPEN, "'['");

    args.add(new MergeExpr(tokenizer).optimize());
    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();
      args.add(new MergeExpr(tokenizer).optimize());
    }

    expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'");
  }

  @Override
  public void analyze()
  {
    lut = (LutStmt) findEntity(LutStmt.class, "lut");
    size = lut.size;

    if (lut.inputSize > args.size())
    {
      error("Missing argument for LUT: " + name);
    }

    if (lut.inputSize < args.size())
    {
      error("Extra argument to LUT: " + name);
    }

    for (final BaseExpr arg : args)
    {
      arg.analyze();
      if (arg.size != 1)
      {
        arg.error("LUT argument should be a single trit");
      }
    }
  }

  @Override
  public BaseExpr append()
  {
    append(name);

    boolean first = true;
    for (final BaseExpr arg : args)
    {
      append(first ? "[" : ", ").append(arg);
      first = false;
    }

    return append("]");
  }

  @Override
  public BaseExpr clone()
  {
    return new LutExpr(this);
  }

  @Override
  public void eval(final CodeContext context)
  {
    context.evalLutLookup(this);
  }
}
