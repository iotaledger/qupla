package org.iota.qupla.qupla.expression;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class VectorExpr extends BaseExpr
{
  public TritVector vector;

  public VectorExpr(final VectorExpr copy)
  {
    super(copy);

    vector = new TritVector(copy.vector);
  }

  public VectorExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    vector = new TritVector(0, '@');

    name = "";
    if (tokenizer.tokenId() == Token.TOK_MINUS)
    {
      name = "-";
      tokenizer.nextToken();
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

    // are we assigning to a known type?
    if (constTypeInfo == null)
    {
      vector = new TritVector(TritConverter.fromDecimal(name));
      size = vector.size();
      return;
    }

    // is this a float type?
    if (constTypeInfo.isFloat)
    {
      final BaseExpr mantissa = constTypeInfo.struct.fields.get(0);
      final BaseExpr exponent = constTypeInfo.struct.fields.get(1);
      vector = new TritVector(TritConverter.fromFloat(name, mantissa.size, exponent.size));
      size = mantissa.size + exponent.size;
      return;
    }

    if (name.indexOf('.') >= 0)
    {
      error("Unexpected float constant: " + name);
    }

    vector = new TritVector(TritConverter.fromDecimal(name));
    size = vector.size();
    if (size > constTypeInfo.size)
    {
      error("Constant value '" + name + "' exceeds " + constTypeInfo.size + " trits");
    }

    size = constTypeInfo.size;
    if (vector.size() < size)
    {
      vector = TritVector.concat(vector, new TritVector(size - vector.size(), '0'));
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new VectorExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalVector(this);
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
