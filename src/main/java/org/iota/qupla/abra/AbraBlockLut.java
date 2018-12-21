package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.statement.LutStmt;
import org.iota.qupla.statement.helper.LutEntry;

public class AbraBlockLut extends AbraBlock
{
  public int tritNr;

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context);
  }

  @Override
  public void code()
  {
    final LutStmt lut = (LutStmt) origin;
    tritCode.putTrit("?01-".charAt(lut.inputSize));
    tritCode.putInt(lut.entries.size());
    for (final LutEntry entry : lut.entries)
    {
      tritCode.putTrits(entry.inputs);
      tritCode.putTrit(entry.outputs.charAt(tritNr));
    }
  }

  @Override
  public String type()
  {
    return "[]";
  }
}
