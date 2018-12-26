package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;

public class AbraSiteMerge extends AbraSite
{
  public ArrayList<AbraSite> inputs = new ArrayList<>();
  public String type = "merge";
  public char typeTrit = '1';

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).append(type + "(");

    boolean first = true;
    for (AbraSite input : inputs)
    {
      context.append(first ? "" : ", ").append("" + refer(input.index));
      first = false;
    }

    return context.append(")");
  }

  @Override
  public void code(final TritCode tritCode)
  {
    tritCode.putTrit(typeTrit);
    tritCode.putInt(inputs.size());
    for (final AbraSite input : inputs)
    {
      tritCode.putInt(refer(input.index));
    }
  }

  @Override
  public void markReferences()
  {
    super.markReferences();

    for (int i = 0; i < inputs.size(); i++)
    {
      final AbraSite input = inputs.get(i);
      if (input instanceof AbraSiteState)
      {
        // reroute from placeholder to actual latch site
        final AbraSiteState state = (AbraSiteState) input;
        inputs.set(i, state.latch);
        state.latch.references++;
        continue;
      }

      input.references++;
    }
  }
}
