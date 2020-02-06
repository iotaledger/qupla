package org.iota.qupla.abra.block.site;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.helper.TritVector;

public class AbraSiteKnot extends AbraBaseSite
{
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
    if (block.index != knot.block.index)
    {
      return false;
    }

    if (block.specialType != 0 && !block.name.equals(knot.block.name))
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

  public void vector(final TritVector vector)
  {
    final AbraBlockBranch constant = new AbraBlockBranch();
    constant.specialType = AbraBaseBlock.TYPE_CONSTANT;
    constant.index = constant.specialType;
    if (vector.isZero())
    {
      constant.name = "constZero_" + size;
    }
    else
    {
      final String trits = vector.trits().replace('-', 'T');
      int len = trits.length();
      while (trits.charAt(len - 1) == '0')
      {
        len--;
      }

      constant.name = "const_" + size + "_" + trits.substring(0, len);
    }

    constant.size = size;
    constant.constantValue = vector;
    block = constant;
  }
}
