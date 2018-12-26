package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.context.AbraContext;

public class EmptyFunctionOptimizer extends BaseOptimizer
{
  public EmptyFunctionOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    //TODO find and disable all function calls that do nothing
  }
}
