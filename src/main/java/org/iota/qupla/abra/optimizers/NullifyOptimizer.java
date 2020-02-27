package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

//TODO split multi-referenced knots into clones that each have only one reference
//     to give NullifyOptimizer maximal optimization possibilities

public class NullifyOptimizer extends BaseOptimizer
{
  public NullifyOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
    branch.numberSites();
  }

  private boolean isNullify(final AbraSiteKnot knot)
  {
    return knot.block.index == AbraBlockSpecial.TYPE_NULLIFY_TRUE || //
        knot.block.index == AbraBlockSpecial.TYPE_NULLIFY_FALSE;
  }

  protected void moveNullify(final AbraSiteKnot nullify, final AbraSiteKnot knot)
  {
    final AbraBaseSite constant = nullify.inputs.get(0);
    for (int i = knot.inputs.size() - 1; i >= 0; i--)
    {
      final AbraBaseSite input = knot.inputs.get(i);
      final AbraSiteKnot newNullify = new AbraSiteKnot();
      newNullify.size = input.size;
      newNullify.block = new AbraBlockSpecial(nullify.block.index, newNullify.size);
      newNullify.inputs.add(constant);
      constant.references++;
      newNullify.inputs.add(input);
      knot.inputs.set(i, newNullify);
      newNullify.references++;
      branch.sites.add(knot.index - branch.sites.get(0).index, newNullify);
    }

    replaceSite(nullify, knot);

    // this could have freed up another optimization possibility,
    // so we restart the optimization from the end
    index = 0;
    branch.numberSites();
  }

  @Override
  protected void processKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    if (!isNullify(knot))
    {
      // only process nullifiers
      return;
    }

    final AbraBaseSite target = knot.inputs.get(1);
    if (!(target instanceof AbraSiteKnot))
    {
      // only move nullifier when it targets a knot
      return;
    }

    final AbraSiteKnot targetKnot = (AbraSiteKnot) target;
    //if (isNullify(targetKnot) || targetKnot.block.index == AbraBlockSpecial.TYPE_CONST)
    if (targetKnot.block instanceof AbraBlockSpecial)
    {
      // cannot move when nullifier, const, or merge
      //TODO handle concat, slice
      return;
    }

    if (targetKnot.references != 1)
    {
      // cannot nullify something that is used elsewhere
      return;
    }

    //TODO handle moving of targetKnot when knot comes before constant
    final AbraBaseSite constant = knot.inputs.get(0);
    if (targetKnot.index > constant.index)
    {
      moveNullify(knot, targetKnot);
    }
  }
}
