package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.helper.TritVector;

public class MultiLutOptimizer extends BaseOptimizer
{
  private static final byte[] POWER_OF_3 = {
      1,
      3,
      9,
      27
  };
  private static final byte[] TRIT = {
      TritVector.TRIT_MIN,
      TritVector.TRIT_ZERO,
      TritVector.TRIT_ONE
  };

  private final HashMap<AbraBaseSite, Byte> values = new HashMap<>();

  public MultiLutOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
    reverse = true;
  }

  private AbraBlockLut generateLookupTable(final AbraSiteKnot master, final AbraSiteKnot slave, final ArrayList<AbraBaseSite> inputs)
  {
    // initialize with 27 null trits
    final byte[] lookup = new byte[27];
    Arrays.fill(lookup, TritVector.TRIT_NULL);

    final int lookupSize = POWER_OF_3[inputs.size()];
    for (int v = 0; v < lookupSize; v++)
    {
      int value = v;
      for (int i = 0; i < inputs.size(); i++)
      {
        values.put(inputs.get(i), TRIT[value % 3]);
        value /= 3;
      }

      final byte slaveTrit = lookupTrit(slave);
      if (slaveTrit == TritVector.TRIT_NULL)
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

    final AbraBlockLut newLut = new AbraBlockLut(lookup);
    final String lutName = newLut.unnamed();

    final AbraBlockLut lut = module.findLut(lutName);
    return lut != null ? lut : module.addLut(lutName, newLut);
  }

  private byte lookupTrit(final AbraSiteKnot lut)
  {
    if (lut.block.index == AbraBlockSpecial.TYPE_CONST)
    {
      final AbraBlockSpecial block = (AbraBlockSpecial) lut.block;
      return block.constantValue.trit(0);
    }

    int index = 0;
    int power = 1;
    for (int i = 0; i < lut.inputs.size(); i++)
    {
      final byte trit = values.get(lut.inputs.get(i));
      if (trit != TritVector.TRIT_MIN)
      {
        index += trit == TritVector.TRIT_ONE ? power * 2 : power;
      }

      power *= 3;
    }

    // look up the trit in the lut lookup table
    return ((AbraBlockLut) lut.block).lookup(index);
  }

  private boolean mergeLuts(final AbraSiteKnot master, final AbraSiteKnot slave)
  {
    if (!(slave.block instanceof AbraBlockLut))
    {
      if (slave.block.index != AbraBlockSpecial.TYPE_CONST)
      {
        return false;
      }

      final AbraBlockSpecial block = (AbraBlockSpecial) slave.block;
      if (block.size != 1)
      {
        return false;
      }
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

      // too many inputs to combine LUTs?
      if (inputs.size() > 3)
      {
        return false;
      }
    }

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
  protected void processKnotLut(final AbraSiteKnot knot, final AbraBlockLut lut)
  {
    // figure out if this LUT refers to another LUT
    for (final AbraBaseSite input : knot.inputs)
    {
      if (input instanceof AbraSiteKnot && mergeLuts(knot, (AbraSiteKnot) input))
      {
        // this could have freed up another optimization possibility,
        // so we restart the optimization from the end
        index = branch.sites.size();
        return;
      }
    }
  }
}
