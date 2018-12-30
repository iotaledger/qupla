package org.iota.qupla.expression;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class IntegerExpr extends BaseExpr
{
  public final TritVector vector;

  public IntegerExpr(final IntegerExpr copy)
  {
    super(copy);

    vector = new TritVector(copy.vector);
  }

  public IntegerExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    vector = new TritVector();

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
      vector.fromDecimal(name);
      size = vector.size();
      return;
    }

    // is this a float type?
    if (constTypeInfo.isFloat)
    {
      final BaseExpr mantissa = constTypeInfo.struct.fields.get(0);
      final BaseExpr exponent = constTypeInfo.struct.fields.get(1);
      final String errMsg = vector.fromFloat(name, mantissa.size, exponent.size);
      if (errMsg != null)
      {
        error(errMsg);
      }

      size = mantissa.size + exponent.size;
      return;
    }

    if (name.indexOf('.') >= 0)
    {
      error("Unexpected float constant: " + name);
    }

    vector.fromDecimal(name);
    size = vector.size();
    if (size > constTypeInfo.size)
    {
      error("Constant value '" + name + "' exceeds " + constTypeInfo.size + " trits");
    }

    size = constTypeInfo.size;
    if (vector.size() < size)
    {
      vector.padZero(size);
    }
  }

  @Override
  public BaseExpr append()
  {
    return append(name);
  }

  @Override
  public BaseExpr clone()
  {
    return new IntegerExpr(this);
  }

  @Override
  public void eval(final CodeContext context)
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
