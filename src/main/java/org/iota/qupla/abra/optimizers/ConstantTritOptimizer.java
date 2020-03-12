package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class ConstantTritOptimizer extends BaseOptimizer
{
  private final AbraBlockLut lutFalse;
  private final AbraBlockLut lutMin;
  private final AbraBlockLut lutOne;
  private final AbraBlockLut lutTrue;
  private final AbraBlockLut lutZero;

  public ConstantTritOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);

    lutZero = module.luts.get(0);
    lutOne = module.luts.get(1);
    lutMin = module.luts.get(2);
    lutTrue = module.luts.get(3);
    lutFalse = module.luts.get(4);
  }

  @Override
  protected void processKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    if (block.size != 1)
    {
      return;
    }

    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      knot.block = lutTrue;
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
      knot.block = lutFalse;
      break;
    }
  }
}
