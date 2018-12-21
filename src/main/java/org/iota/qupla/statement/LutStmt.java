package org.iota.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;
import org.iota.qupla.statement.helper.LutEntry;

public class LutStmt extends BaseExpr
{
  private static int[] tableSize = {
      0,
      3,
      9,
      27,
      81,
      243,
      729,
      2187,
      6561,
      19683
  };

  public final ArrayList<LutEntry> entries = new ArrayList<>();
  public int inputSize;
  public TritVector[] lookup;
  public TritVector undefined;

  public LutStmt(final LutStmt copy)
  {
    super(copy);

    entries.addAll(copy.entries);
    inputSize = copy.inputSize;
    lookup = copy.lookup;
    undefined = copy.undefined;
  }

  public LutStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_LUT, "lut");

    final Token lutName = expect(tokenizer, Token.TOK_NAME, "LUT name");
    name = lutName.text;
    module.checkDuplicateName(module.luts, this);

    expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'");

    do
    {
      entries.add(new LutEntry(tokenizer));
    }
    while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE);

    tokenizer.nextToken();
  }

  @Override
  public void analyze()
  {
    final LutEntry first = entries.get(0);
    inputSize = first.inputs.length();
    size = first.outputs.length();
    lookup = new TritVector[tableSize[inputSize]];
    for (final LutEntry entry : entries)
    {
      entry.analyze();
      if (entry.inputs.length() != inputSize)
      {
        entry.error("Expected " + inputSize + " input trits");
      }

      if (entry.outputs.length() != size)
      {
        entry.error("Expected " + size + " output trits");
      }

      final TritVector input = new TritVector(entry.inputs, inputSize);
      final int lutIndex = input.toLutIndex();
      if (lookup[lutIndex] != null)
      {
        entry.error("Duplicate input trits");
      }

      lookup[lutIndex] = new TritVector(entry.outputs, size);
    }

    undefined = new TritVector(size);
  }

  @Override
  public BaseExpr append()
  {
    append("lut ").append(name).append(" [").newline().indent();

    for (final LutEntry entry : entries)
    {
      append(entry).newline();
    }

    return undent().append("]");
  }

  @Override
  public BaseExpr clone()
  {
    return new LutStmt(this);
  }

  @Override
  public String toString()
  {
    return "lut " + name;
  }
}
