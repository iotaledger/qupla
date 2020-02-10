package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class NullifyInserter extends BaseOptimizer
{
  public NullifyInserter(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private void insertNullify(final AbraSiteKnot knot, final AbraBaseSite condition, final int type)
  {
    final AbraSiteKnot nullify = new AbraSiteKnot();
    nullify.size = knot.size;
    nullify.block = new AbraBlockSpecial(type, nullify.size);
    nullify.inputs.add(condition);

    knot.nullifyFalse = null;
    knot.nullifyTrue = null;
    branch.sites.add(index + 1, nullify);

    replaceSite(knot, nullify);
    nullify.inputs.add(knot);
    knot.references++;
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    if (knot.references == 0)
    {
      return;
    }

    if (knot.nullifyFalse != null)
    {
      insertNullify(knot, knot.nullifyFalse, AbraBlockSpecial.TYPE_NULLIFY_FALSE);
      return;
    }

    if (knot.nullifyTrue != null)
    {
      insertNullify(knot, knot.nullifyTrue, AbraBlockSpecial.TYPE_NULLIFY_TRUE);
    }
  }
}
