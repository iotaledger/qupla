package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.context.AbraCodeContext;

public class AbraBlockImport extends AbraBlock
{
  public ArrayList<AbraBlock> blocks = new ArrayList<>();
  public String hash;

  @Override
  public void code()
  {
    tritCode.putTrits(hash);
    tritCode.putInt(blocks.size());
    for (final AbraBlock block : blocks)
    {
      tritCode.putInt(block.index);
    }
  }

  @Override
  public void eval(final AbraCodeContext context)
  {
    context.evalImport(this);
  }

  @Override
  public String type()
  {
    return "<<";
  }
}
