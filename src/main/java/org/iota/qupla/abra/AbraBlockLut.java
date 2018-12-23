package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.statement.LutStmt;
import org.iota.qupla.statement.helper.LutEntry;

public class AbraBlockLut extends AbraBlock
{
  public static final int[] powers = {
      1,
      3,
      9,
      27
  };
  public int tritNr;

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append("  ").append(new String(tritCode.buffer, 0, 27));
  }

  @Override
  public void code()
  {
    if (tritCode.bufferOffset > 0)
    {
      return;
    }

    // initialize with 27 null trits
    final char[] lookup = new TritVector(27).trits.toCharArray();

    final LutStmt lut = (LutStmt) origin;
    for (final LutEntry entry : lut.entries)
    {
      // build index for this entry in lookup table
      int index = 0;
      for (int i = 0; i < entry.inputs.length(); i++)
      {
        final char c = entry.inputs.charAt(i);
        final int v = c == '-' ? 0 : c == '0' ? 1 : 2;
        index += v * powers[i];
      }

      // set corresponding character
      lookup[index] = entry.outputs.charAt(tritNr);
    }

    // repeat the entries across the entire table if necessary
    final String trits = new String(lookup, 0, powers[lut.inputSize]);
    for (int offset = 0; offset < 27; offset += trits.length())
    {
      tritCode.putTrits(trits);
    }

    //TODO convert 27 bct 'trits' to 35 trits
  }

  @Override
  public String type()
  {
    return "[]";
  }
}
