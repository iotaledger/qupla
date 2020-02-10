package org.iota.qupla.abra.block.site;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

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

    if (block instanceof AbraBlockSpecial && !block.name.equals(knot.block.name))
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

  @Override
  public void markReferences()
  {
    super.markReferences();

    for (final AbraBaseSite input : inputs)
    {
      input.references++;
    }
  }
}
