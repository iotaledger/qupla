package org.iota.qupla.abra.block.site;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.funcmanagers.ConstFuncManager;
import org.iota.qupla.helper.TritVector;

public class AbraSiteKnot extends AbraBaseSite
{
  public static final ConstFuncManager constants = new ConstFuncManager();
  public AbraBaseBlock block;
  public ArrayList<AbraBaseSite> inputs = new ArrayList<>();

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

    if (inputs.size() != knot.inputs.size())
    {
      return false;
    }

    for (int i = 0; i < inputs.size(); i++)
    {
      if (inputs.get(i) != knot.inputs.get(i))
      {
        return false;
      }
    }

    return true;
  }

  public void lut(final AbraModule module, final String lutName)
  {
    for (final AbraBaseBlock lut : module.luts)
    {
      if (lut.name.equals(lutName))
      {
        block = lut;
        break;
      }
    }
  }

  @Override
  public void markReferences()
  {
    super.markReferences();

    for (final AbraBaseSite input : inputs)
    {
      input.references++;
    }
  }

  public void merge(final AbraModule module)
  {
    //TODO inputSize is not used?????
    block = module.luts.get(0);
  }

  public void nullify(final AbraModule module, final boolean trueFalse)
  {
    final int nullId = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    block = module.luts.get(nullId);
  }

  public void slice(final int start)
  {
    final AbraBlockBranch slice = new AbraBlockBranch();
    slice.specialType = AbraBaseBlock.TYPE_SLICE;
    slice.index = slice.specialType;
    slice.name = "slice_" + size + "_" + start;
    slice.size = size;
    slice.offset = start;
    block = slice;
  }

  public void vector(final AbraModule module, final TritVector vector)
  {
    block = constants.find(module, vector);
  }
}
