package org.iota.qupla.abra;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.abra.funcs.ConstManager;
import org.iota.qupla.abra.funcs.NullifyManager;
import org.iota.qupla.abra.funcs.SliceManager;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;
import org.iota.qupla.helper.TritVector;

public class AbraSiteKnot extends AbraSiteMerge
{
  public static ConstManager constants = new ConstManager();
  public static NullifyManager nullifyFalse = new NullifyManager(false);
  public static NullifyManager nullifyTrue = new NullifyManager(true);
  public static SliceManager slicers = new SliceManager();

  public AbraBlock block;

  public AbraSiteKnot()
  {
    type = "knot";
    typeTrit = '-';
  }

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append(" " + block);
  }

  public void branch(final AbraContext context)
  {
    for (final AbraBlock branch : context.abra.branches)
    {
      if (branch.name.equals(name))
      {
        block = branch;
        break;
      }
    }
  }

  @Override
  public void code(final TritCode tritCode)
  {
    super.code(tritCode);

    tritCode.putInt(block.index);
  }

  public void concat(final AbraContext context)
  {
    block = slicers.find(context, size, 0);
  }

  @Override
  public void eval(final AbraCodeContext context)
  {
    context.evalKnot(this);
  }

  public void lut(final AbraContext context)
  {
    for (final AbraBlock lut : context.abra.luts)
    {
      if (lut.name.equals(name))
      {
        block = lut;
        break;
      }
    }
  }

  public void nullify(final AbraContext context, final boolean trueFalse)
  {
    final NullifyManager nullify = trueFalse ? nullifyTrue : nullifyFalse;
    block = nullify.find(context, size);
  }

  public void slice(final AbraContext context, final int inputSize, final int start)
  {
    block = slicers.find(context, size, start);
  }

  public void vector(final AbraContext context, final TritVector vector)
  {
    block = constants.find(context, vector);
  }
}
