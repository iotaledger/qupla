package org.iota.qupla.abra;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;

public abstract class AbraBlock
{
  public int index;
  public String name;
  public BaseExpr origin;
  public TritCode tritCode = new TritCode();

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

  public void optimize(final AbraContext context)
  {
  }

  @Override
  public String toString()
  {
    return "block " + index + " // " + name + type();
  }

  public abstract String type();
}
