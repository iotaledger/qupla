package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.funcmanagers.ConstFuncManager;
import org.iota.qupla.abra.funcmanagers.NullifyFuncManager;
import org.iota.qupla.abra.funcmanagers.SliceFuncManager;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class AbraSiteKnot extends AbraSiteMerge
{
  public static ConstFuncManager constants = new ConstFuncManager();
  public static NullifyFuncManager nullifyFalse = new NullifyFuncManager(false);
  public static NullifyFuncManager nullifyTrue = new NullifyFuncManager(true);
  public static SliceFuncManager slicers = new SliceFuncManager();

  public AbraBaseBlock block;

  public void branch(final QuplaToAbraContext context)
  {
    for (final AbraBaseBlock branch : context.abraModule.branches)
    {
      if (branch.name.equals(name))
      {
        block = branch;
        break;
      }
    }
  }

  public void concat(final QuplaToAbraContext context)
  {
    block = slicers.find(context, size, 0);
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalKnot(this);
  }

  public void lut(final QuplaToAbraContext context)
  {
    for (final AbraBaseBlock lut : context.abraModule.luts)
    {
      if (lut.name.equals(name))
      {
        block = lut;
        break;
      }
    }
  }

  public void nullify(final QuplaToAbraContext context, final boolean trueFalse)
  {
    final NullifyFuncManager nullify = trueFalse ? nullifyTrue : nullifyFalse;
    block = nullify.find(context, size);
  }

  public void slice(final QuplaToAbraContext context, final int inputSize, final int start)
  {
    block = slicers.find(context, size, start);
  }

  public void vector(final QuplaToAbraContext context, final TritVector vector)
  {
    block = constants.find(context, vector);
  }
}
