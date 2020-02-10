package org.iota.qupla.abra.context.base;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
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

    //TODO determine correct order, imports first or last?
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

  public void evalKnot(final AbraSiteKnot knot)
  {
    if (knot.block instanceof AbraBlockSpecial)
    {
      evalKnotSpecial(knot, (AbraBlockSpecial) knot.block);
      return;
    }

    if (knot.block instanceof AbraBlockBranch)
    {
      evalKnotBranch(knot, (AbraBlockBranch) knot.block);
      return;
    }

    if (knot.block instanceof AbraBlockLut)
    {
      evalKnotLut(knot, (AbraBlockLut) knot.block);
      return;
    }

    error("WTF?");
  }

  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
  }

  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
  }

  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
  }

  public abstract void evalLatch(final AbraSiteLatch latch);

  public abstract void evalLut(final AbraBlockLut lut);

  public abstract void evalParam(final AbraSiteParam param);

  public abstract void evalSpecial(final AbraBlockSpecial block);
}
