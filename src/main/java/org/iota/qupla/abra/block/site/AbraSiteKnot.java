package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.funcmanagers.ConstFuncManager;
import org.iota.qupla.abra.funcmanagers.NullifyFuncManager;
import org.iota.qupla.abra.funcmanagers.SliceFuncManager;
import org.iota.qupla.helper.TritVector;

public class AbraSiteKnot extends AbraSiteMerge
{
  public static final ConstFuncManager constants = new ConstFuncManager();
  public static final NullifyFuncManager nullifyFalse = new NullifyFuncManager(false);
  public static final NullifyFuncManager nullifyTrue = new NullifyFuncManager(true);
  public static final SliceFuncManager slicers = new SliceFuncManager();

  public AbraBaseBlock block;

  public void branch(final AbraModule module)
  {
    for (final AbraBaseBlock branch : module.branches)
    {
      if (branch.name.equals(name))
      {
        block = branch;
        break;
      }
    }
  }

  public void concat(final AbraModule module)
  {
    block = slicers.find(module, size, 0);
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalKnot(this);
  }

  public void lut(final AbraModule module)
  {
    for (final AbraBaseBlock lut : module.luts)
    {
      if (lut.name.equals(name))
      {
        block = lut;
        break;
      }
    }
  }

  public void nullify(final AbraModule module, final boolean trueFalse)
  {
    final NullifyFuncManager nullify = trueFalse ? nullifyTrue : nullifyFalse;
    block = nullify.find(module, size);
  }

  public void slice(final AbraModule module, final int inputSize, final int start)
  {
    //TODO inputSize is not used?????
    block = slicers.find(module, size, start);
  }

  public void vector(final AbraModule module, final TritVector vector)
  {
    block = constants.find(module, vector);
  }
}
