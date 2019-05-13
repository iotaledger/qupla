package org.iota.qupla.abra.context.base;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.BaseContext;

public abstract class AbraBaseContext extends BaseContext
{
  protected void error(final String text)
  {
    throw new CodeException(text);
  }

  public void eval(final AbraModule module)
  {
    module.numberBlocks();

    evalBlocks(module.imports);
    evalBlocks(module.luts);
    evalBlocks(module.branches);
  }

  protected void evalBlocks(final ArrayList<? extends AbraBaseBlock> blocks)
  {
    for (final AbraBaseBlock block : blocks)
    {
      block.eval(this);
    }
  }

  public abstract void evalBranch(final AbraBlockBranch branch);

  public abstract void evalImport(final AbraBlockImport imp);

  public abstract void evalKnot(final AbraSiteKnot knot);

  public abstract void evalLatch(final AbraSiteLatch latch);

  public abstract void evalLut(final AbraBlockLut lut);

  public abstract void evalMerge(final AbraSiteMerge merge);

  public abstract void evalParam(final AbraSiteParam param);
}
