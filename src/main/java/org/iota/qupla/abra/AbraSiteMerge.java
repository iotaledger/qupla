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

  @Override
  public void optimizeNullify()
  {
    // check if all inputs have only a single reference
    for (final AbraSite input : inputs)
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
    if (nullifyFalse != null)
    {
      for (final AbraSite input : inputs)
      {
        input.nullifyFalse = nullifyFalse;
        nullifyFalse.references++;
        input.optimizeNullify();
      }

      nullifyFalse.references--;
      nullifyFalse = null;
    }

    // move nullifyTrue up the chain??
    if (nullifyTrue != null)
    {
      nullifyTrue.references--;
      for (final AbraSite input : inputs)
      {
        input.nullifyTrue = nullifyTrue;
        nullifyTrue.references++;
        input.optimizeNullify();
      }

      nullifyTrue.references--;
      nullifyTrue = null;
    }
  }
}
