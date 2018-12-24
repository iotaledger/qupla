package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.funcs.ConcatManager;
import org.iota.qupla.abra.funcs.ConstManager;
import org.iota.qupla.abra.funcs.NullifyManager;
import org.iota.qupla.abra.funcs.SliceManager;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraSiteKnot extends AbraSite
{
  public static ConcatManager concats = new ConcatManager();
  public static ConstManager constants = new ConstManager();
  public static NullifyManager nullifyFalse = new NullifyManager(false);
  public static NullifyManager nullifyTrue = new NullifyManager(true);
  public static SliceManager slicers = new SliceManager();

  public AbraBlock block;
  public ArrayList<AbraSite> inputs = new ArrayList<>();

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).append("knot(");
    boolean first = true;
    for (AbraSite input : inputs)
    {
      context.append(first ? "" : ", ").append("" + refer(input.index));
      first = false;
    }

    return context.append(") " + block);
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
    tritCode.putTrit('-');
    tritCode.putInt(inputs.size());
    for (final AbraSite input : inputs)
    {
      tritCode.putInt(refer(input.index));
    }

    tritCode.putInt(block.index);
  }

  public void concat(final AbraContext context)
  {
    block = concats.find(context, size);
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

  public void nullifyFalse(final AbraContext context)
  {
    block = nullifyFalse.find(context, size);
  }

  public void nullifyTrue(final AbraContext context)
  {
    block = nullifyTrue.find(context, size);
  }

  public void slice(final AbraContext context, final int inputSize, final int start)
  {
    if (start == 0)
    {
      block = concats.find(context, size);
      return;
    }

    block = slicers.find(context, size, start);
  }

  public void vector(final AbraContext context, final String trits)
  {
    block = constants.find(context, trits);
  }
}
