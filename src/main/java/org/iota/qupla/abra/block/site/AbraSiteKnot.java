package org.iota.qupla.abra.block.site;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.funcmanagers.ConstFuncManager;
import org.iota.qupla.abra.funcmanagers.MergeFuncManager;
import org.iota.qupla.abra.funcmanagers.NullifyFuncManager;
import org.iota.qupla.abra.funcmanagers.SliceFuncManager;
import org.iota.qupla.helper.TritVector;

public class AbraSiteKnot extends AbraSiteMerge
{
  public static final ConstFuncManager constants = new ConstFuncManager();
  public static final MergeFuncManager mergers = new MergeFuncManager();
  public static final NullifyFuncManager nullifyFalse = new NullifyFuncManager(false);
  public static final NullifyFuncManager nullifyTrue = new NullifyFuncManager(true);
  public static final SliceFuncManager slicers = new SliceFuncManager();
  public AbraBaseBlock block;

  public AbraSiteKnot()
  {
  }

  public AbraSiteKnot(final AbraSiteKnot copy)
  {
    super(copy);
    block = copy.block;
  }

  @Override
  public AbraBaseSite clone()
  {
    return new AbraSiteKnot(this);
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

  @Override
  public boolean isIdentical(final AbraBaseSite rhs)
  {
    if (!super.isIdentical(rhs))
    {
      return false;
    }

    final AbraSiteKnot knot = (AbraSiteKnot) rhs;
    if (block != knot.block)
    {
      return false;
    }

    return true;
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

  public void merge(final AbraModule module, final int inputSize)
  {
    //TODO inputSize is not used?????
    block = mergers.find(module, size);
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
