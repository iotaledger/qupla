package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class NullifyOptimizer extends BaseOptimizer
{
  public NullifyOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
    reverse = true;
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    if (index == 0 || !knot.hasNullifier() || knot.block.index == AbraBlockSpecial.TYPE_CONST)
    {
      // no need to move it up the chain anyway
      return;
    }

    // check if all inputs have only a single reference
    final ArrayList<AbraSiteKnot> inputs = new ArrayList<>(knot.inputs.size());
    for (final AbraBaseSite input : knot.inputs)
    {
      if (input.hasNullifier())
      {
        // cannot force a nullify on something that already has one
        return;
      }

      //TODO when at least one input has a single reference
      //     insert knots for the inputs that have >1 references
      //     and then move the nullifies there to avoid calling the knot
      if (input.references != 1 || !(input instanceof AbraSiteKnot))
      {
        // cannot force nullify on something referenced from somewhere else
        // nor on something that isn't a merge or a knot
        return;
      }

      inputs.add((AbraSiteKnot) input);
    }

    branch.sites.removeAll(inputs);
    branch.sites.addAll(index - knot.inputs.size(), inputs);

    // move nullifyFalse up the chain??
    if (knot.nullifyFalse != null)
    {
      for (final AbraBaseSite input : knot.inputs)
      {
        input.nullifyFalse = knot.nullifyFalse;
        knot.nullifyFalse.references++;
      }

      knot.nullifyFalse.references--;
      knot.nullifyFalse = null;
    }

    // move nullifyTrue up the chain??
    if (knot.nullifyTrue != null)
    {
      for (final AbraBaseSite input : knot.inputs)
      {
        input.nullifyTrue = knot.nullifyTrue;
        knot.nullifyTrue.references++;
      }

      knot.nullifyTrue.references--;
      knot.nullifyTrue = null;
    }

    // this could have freed up another optimization possibility,
    // so we restart the optimization from the end
    index = branch.sites.size();
  }
}
