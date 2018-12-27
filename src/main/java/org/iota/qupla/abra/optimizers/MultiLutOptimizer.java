package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.HashMap;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.helper.TritVector;

public class MultiLutOptimizer extends BaseOptimizer
{
  public HashMap<AbraSite, Character> values = new HashMap<>();

  public MultiLutOptimizer(final AbraContext context, final AbraBlockBranch branch)
  {
    super(context, branch);
    reverse = true;
  }

  private AbraBlock generateLookupTable(final AbraSiteKnot master, final AbraSiteKnot slave, final ArrayList<AbraSite> inputs)
  {
    // initialize lookup tables if necessary
    master.block.code();
    slave.block.code();

    // initialize with 27 null trits
    final char[] lookup = new TritVector(27).trits.toCharArray();

    final int maxValue = AbraBlockLut.powers[inputs.size()];
    for (int v = 0; v < maxValue; v++)
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
    for (int offset = maxValue; offset < 27; offset += maxValue)
    {
      for (int i = 0; i < maxValue; i++)
      {
        lookup[offset + i] = lookup[i];
      }
    }

    final String trits = new String(lookup, 0, 27);

    final AbraSiteKnot tmp = new AbraSiteKnot();
    tmp.name = "$lut$" + trits.replace('-', 'T').replace('@', 'N');
    tmp.lut(context);

    // already exists?
    if (tmp.block != null)
    {
      return tmp.block;
    }

    // new LUT, create it
    return context.abra.addLut(tmp.name, trits);
  }

  private char lookupTrit(final AbraSiteKnot lut)
  {
    int index = 0;
    for (int i = 0; i < lut.inputs.size(); i++)
    {
      final char trit = values.get(lut.inputs.get(i));
      final int val = trit == '-' ? 0 : trit == '0' ? 1 : 2;
      index += val * AbraBlockLut.powers[i];
    }

    // look up the trit in the slave lookup table
    return lut.block.tritCode.buffer[index];
  }

  private boolean mergeLuts(final AbraSiteKnot master, final AbraSiteKnot slave)
  {
    final ArrayList<AbraSite> inputs = new ArrayList<>();

    // gather all unique master inputs (omit slave)
    for (final AbraSite input : master.inputs)
    {
      if (input != slave && !inputs.contains(input))
      {
        inputs.add(input);
        continue;
      }
    }

    // gather all unique slave inputs
    for (final AbraSite input : slave.inputs)
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
    for (final AbraSite input : knot.inputs)
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
