package org.iota.qupla.abra.block.site;

import java.util.ArrayList;

import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraSiteMerge extends AbraBaseSite
{
  public ArrayList<AbraBaseSite> inputs = new ArrayList<>();

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalMerge(this);
  }

  @Override
  public void markReferences()
  {
    super.markReferences();

    for (int i = 0; i < inputs.size(); i++)
    {
      final AbraBaseSite input = inputs.get(i);
      if (input instanceof AbraSiteLatch)
      {
        // reroute from placeholder to actual latch site
        final AbraSiteLatch state = (AbraSiteLatch) input;
        inputs.set(i, state.latch);
        state.latch.references++;
        continue;
      }

      input.references++;
    }
  }
}
