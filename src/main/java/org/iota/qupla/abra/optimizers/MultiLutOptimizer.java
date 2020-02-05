package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.HashMap;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class MultiLutOptimizer extends BaseOptimizer
{
  private final HashMap<AbraBaseSite, Character> values = new HashMap<>();

  public MultiLutOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
    reverse = true;
  }

  private AbraBaseBlock generateLookupTable(final AbraSiteKnot master, final AbraSiteKnot slave, final ArrayList<AbraBaseSite> inputs)
  {
    // initialize with 27 null trits
    final char[] lookup = AbraBlockLut.NULL_LUT.toCharArray();

    // powers of 3:              1     3     9    27
    final int lookupSize = "\u0001\u0003\u0009\u001b".charAt(inputs.size());

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
      System.arraycopy(lookup, 0, lookup, offset, lookupSize);
    }

    final String lookupTable = new String(lookup);
    final String lutName = AbraBlockLut.unnamed(lookupTable);

    final AbraSiteKnot knot = new AbraSiteKnot();
    knot.lut(module, lutName);

    // already exists?
    if (knot.block != null)
    {
      return knot.block;
    }

    // new LUT, create it
    return module.addLut(lutName, lookupTable);
  }

  private char lookupTrit(final AbraSiteKnot lut)
  {
    int index = 0;
    int power = 1;
    for (int i = 0; i < lut.inputs.size(); i++)
    {
      final char trit = values.get(lut.inputs.get(i));
      if (trit != '-')
      {
        index += trit == '1' ? power * 2 : power;
      }

      power *= 3;
    }

    // look up the trit in the lut lookup table
    return ((AbraBlockLut) lut.block).lookup.charAt(index);
  }

  private boolean mergeLuts(final AbraSiteKnot master, final AbraSiteKnot slave)
  {
    if (master.block.specialType == AbraBaseBlock.TYPE_MERGE || //
        slave.block.specialType == AbraBaseBlock.TYPE_MERGE)
    {
      return false;
    }

    final ArrayList<AbraBaseSite> inputs = new ArrayList<>();

    // gather all unique master inputs (omit slave)
    for (final AbraBaseSite input : master.inputs)
    {
      if (input.size != 1)
      {
        return false;
      }

      if (input != slave && !inputs.contains(input))
      {
        inputs.add(input);
      }
    }

    // gather all unique slave inputs
    for (final AbraBaseSite input : slave.inputs)
    {
      if (input.size != 1)
      {
        return false;
      }

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

    if (AbraModule.lutAlways3)
    {
      // LUTs always need 3 inputs
      while (inputs.size() < 3)
      {
        inputs.add(inputs.get(0));
      }
    }

    // update master with new inputs
    for (int i = 0; i < master.inputs.size(); i++)
    {
      master.inputs.get(i).references--;
    }

    master.inputs = inputs;
    for (int i = 0; i < master.inputs.size(); i++)
    {
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
