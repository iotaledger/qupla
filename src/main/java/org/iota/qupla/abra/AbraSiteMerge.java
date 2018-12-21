package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;

public class AbraSiteMerge extends AbraSite
{
  public ArrayList<AbraSite> inputs = new ArrayList<>();

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).append("merge(");

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
    tritCode.putTrit('1');
    tritCode.putInt(inputs.size());
    for (final AbraSite input : inputs)
    {
      tritCode.putInt(refer(input.index));
    }
  }
}
