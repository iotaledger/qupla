package org.iota.qupla.abra.context;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockImport;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteLatch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.expression.base.BaseExpr;

public abstract class AbraCodeContext
{
  public abstract void eval(final AbraContext context, final BaseExpr expr);

  public abstract void evalBranch(final AbraBlockBranch branch);

  public abstract void evalImport(final AbraBlockImport imp);

  public abstract void evalKnot(final AbraSiteKnot knot);

  public abstract void evalLatch(final AbraSiteLatch state);

  public abstract void evalLut(final AbraBlockLut lut);

  public abstract void evalMerge(final AbraSiteMerge merge);

  public abstract void evalParam(final AbraSiteParam param);
}
