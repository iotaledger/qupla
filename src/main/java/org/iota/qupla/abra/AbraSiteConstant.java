package org.iota.qupla.abra;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.helper.TritVector;

public class AbraSiteConstant extends AbraSite
{
  public TritVector vector;

  @Override
  public CodeContext append(final CodeContext context)
  {
    return super.append(context).append("constant: " + vector);
  }

  @Override
  public void code(final TritCode tritCode)
  {
    tritCode.putTrit('0');
    tritCode.putInt(vector.trits.length());
    tritCode.putTrits(vector.trits);
  }
}
