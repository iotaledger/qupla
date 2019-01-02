package org.iota.qupla.abra.block.base;

import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public abstract class AbraBaseBlock
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
  public int type;

  public boolean anyNull()
  {
    return false;
  }

  public abstract void eval(final AbraBaseContext context);

  public void markReferences()
  {
  }

  public int offset()
  {
    return 0;
  }

  public void optimize(final QuplaToAbraContext context)
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
