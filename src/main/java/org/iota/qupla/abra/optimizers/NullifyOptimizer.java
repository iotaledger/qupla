package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.context.AbraContext;

public class NullifyOptimizer extends BaseOptimizer
{
  public NullifyOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
  }

  @Override
  protected void processSite(final AbraSiteMerge site)
  {
    // check if all inputs have only a single reference
    for (final AbraSite input : site.inputs)
    {
      if (input.nullifyFalse != null || input.nullifyTrue != null)
      {
        // cannot force a nullify on something that already has one
        return;
      }

      if (input.references != 1 || !(input instanceof AbraSiteMerge))
      {
        // cannot force nullify on something referenced from somewhere else
        // nor on something that isn't a merge or a knot
        return;
      }
    }

    // move nullifyFalse up the chain??
    if (site.nullifyFalse != null)
    {
      for (final AbraSite input : site.inputs)
      {
        input.nullifyFalse = site.nullifyFalse;
        site.nullifyFalse.references++;
        processSite((AbraSiteMerge) input);
      }

      site.nullifyFalse.references--;
      site.nullifyFalse = null;
    }

    // move nullifyTrue up the chain??
    if (site.nullifyTrue != null)
    {
      site.nullifyTrue.references--;
      for (final AbraSite input : site.inputs)
      {
        input.nullifyTrue = site.nullifyTrue;
        site.nullifyTrue.references++;
        processSite((AbraSiteMerge) input);
      }

      site.nullifyTrue.references--;
      site.nullifyTrue = null;
    }
  }
}
