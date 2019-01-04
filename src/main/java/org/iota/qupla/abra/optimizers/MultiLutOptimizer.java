package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.HashMap;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class MultiLutOptimizer extends BaseOptimizer
{
  public HashMap<AbraBaseSite, Character> values = new HashMap<>();

  public MultiLutOptimizer(final QuplaToAbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
    reverse = true;
  }

  private AbraBaseBlock generateLookupTable(final AbraSiteKnot master, final AbraSiteKnot slave, final ArrayList<AbraBaseSite> inputs)
  {
    // initialize with 27 null trits
    final char[] lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@".toCharArray();

    final int lookupSize = QuplaToAbraContext.powers[inputs.size()];
    for (int v = 0; v < lookupSize; v++)
    {
      int value = v;
      for (int i = 0; i < inputs.size(); i++)
      {
        values.put(inputs.get(i), "-01".charAt(value % 3));
        value /= 3;
      }

      final char slaveTrit = lookupTrit(slave);
      if (slaveTrit == '@')
      {
        // null value, no need to continue
        continue;
      }

      values.put(slave, slaveTrit);

      lookup[v] = lookupTrit(master);
    }

    // repeat the entries across the entire table if necessary
    for (int offset = lookupSize; offset < 27; offset += lookupSize)
    {
      for (int i = 0; i < lookupSize; i++)
      {
        lookup[offset + i] = lookup[i];
      }
    }

    final String lookupTable = new String(lookup);

    final AbraSiteKnot tmp = new AbraSiteKnot();
    tmp.name = AbraBlockLut.unnamed(lookupTable);
    tmp.lut(context);

    // already exists?
    if (tmp.block != null)
    {
      return tmp.block;
    }

    // new LUT, create it
    return context.abraModule.addLut(tmp.name, lookupTable);
  }

  private char lookupTrit(final AbraSiteKnot lut)
  {
    int index = 0;
    for (int i = 0; i < lut.inputs.size(); i++)
    {
      final char trit = values.get(lut.inputs.get(i));
      final int val = trit == '-' ? 0 : trit == '0' ? 1 : 2;
      index += val * QuplaToAbraContext.powers[i];
    }

    // look up the trit in the lut lookup table
    return ((AbraBlockLut) lut.block).lookup.charAt(index);
  }

  private boolean mergeLuts(final AbraSiteKnot master, final AbraSiteKnot slave)
  {
    final ArrayList<AbraBaseSite> inputs = new ArrayList<>();

    // gather all unique master inputs (omit slave)
    for (final AbraBaseSite input : master.inputs)
    {
      if (input != slave && !inputs.contains(input))
      {
        inputs.add(input);
        continue;
      }
    }

    // gather all unique slave inputs
    for (final AbraBaseSite input : slave.inputs)
    {
      if (!inputs.contains(input))
      {
        inputs.add(input);
      }
    }

    // too many inputs to combine LUTs?
    if (inputs.size() > 3)
    {
      return false;
    }

    //TODO update block references (not just here)
    // get lookup table for combined LUT
    master.block = generateLookupTable(master, slave, inputs);

    // LUTs always need 3 inputs
    while (inputs.size() < 3)
    {
      inputs.add(inputs.get(0));
    }

    // update master with new inputs
    for (int i = 0; i < 3; i++)
    {
      master.inputs.get(i).references--;
      master.inputs.set(i, inputs.get(i));
      master.inputs.get(i).references++;
    }

    return true;
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    if (!(knot.block instanceof AbraBlockLut))
    {
      // nothing to optimize here
      return;
    }

    // figure out if this LUT refers to another LUT
    for (final AbraBaseSite input : knot.inputs)
    {
      if (input instanceof AbraSiteKnot)
      {
        final AbraSiteKnot inputKnot = (AbraSiteKnot) input;
        if (inputKnot.block instanceof AbraBlockLut && mergeLuts(knot, inputKnot))
        {
          // this could have freed up another optimization possibility,
          // so we restart the optimization from the end
          index = branch.sites.size();
          return;
        }
      }
    }
  }
}
