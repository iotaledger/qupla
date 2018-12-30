package org.iota.qupla.abra;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;

public abstract class AbraBlock
{
  public static final int TYPE_CONSTANT = 3;
  public static final int TYPE_NULLIFY_FALSE = 2;
  public static final int TYPE_NULLIFY_TRUE = 1;
  public static final int TYPE_SLICE = 4;

  public boolean analyzed;
  public TritVector constantValue;
  public int index;
  public String name;
  public BaseExpr origin;
  public TritCode tritCode = new TritCode();
  public int type;

  public boolean anyNull()
  {
    return false;
  }

  public CodeContext append(final CodeContext context)
  {
    context.newline();
    if (origin != null)
    {
      context.append("" + origin).newline();
    }

    return context.append("// " + toString());
  }

  public void code()
  {
  }

  public void code(final TritCode abra)
  {
    code();
    abra.putInt(tritCode.bufferOffset);
    abra.putTrits(new String(tritCode.buffer, 0, tritCode.bufferOffset));
  }

  public abstract void eval(final AbraCodeContext context);

  public void markReferences()
  {
  }

  public int offset()
  {
    return 0;
  }

  public void optimize(final AbraContext context)
  {
  }

  public int size()
  {
    return 1;
  }

  @Override
  public String toString()
  {
    return "block " + index + " // " + name + type();
  }

  public abstract String type();
}
