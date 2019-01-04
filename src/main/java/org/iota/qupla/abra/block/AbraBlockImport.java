package org.iota.qupla.abra.block;

import java.util.ArrayList;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraBlockImport extends AbraBaseBlock
{
  public ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  public String hash;

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalImport(this);
  }
}
