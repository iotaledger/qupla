package org.iota.qupla.qupla.expression;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Tokenizer;

public abstract class VectorExpr extends BaseExpr
{
  public TritVector vector;

  protected VectorExpr(final VectorExpr copy)
  {
    super(copy);

    vector = new TritVector(copy.vector);
  }

  public VectorExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    vector = new TritVector(0, TritVector.TRIT_NULL);

    name = "";
  }

  @Override
  public void analyze()
  {
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
      vector = TritVector.concat(vector, new TritVector(size - vector.size(), TritVector.TRIT_ZERO));
    }
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalVector(this);
  }
}
