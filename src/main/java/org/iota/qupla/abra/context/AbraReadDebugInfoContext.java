package org.iota.qupla.abra.context;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;
import org.iota.qupla.helper.DummyStmt;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraReadDebugInfoContext extends AbraTritCodeBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    evalBlocks(module.luts);
    evalBlocks(module.branches);
    evalBlocks(module.imports);
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    branch.name = getString();
    final String stmt = getString();
    if (stmt != null)
    {
      branch.origin = new DummyStmt(stmt);
    }

    evalBranchSites(branch);
    branch.finalStmt = getStmt();
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    //TODO origin?
    imp.name = getString();
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    evalSite(knot);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch);
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    lut.name = getString();
    if (lut.name == null)
    {
      lut.name = AbraBlockLut.unnamed(lut.lookup);
    }

    final String stmt = getString();
    if (stmt != null)
    {
      lut.origin = new DummyStmt(stmt);
    }
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param);
  }

  private void evalSite(final AbraBaseSite site)
  {
    //TODO putInt(typeId) (index of origin.typeInfo)
    site.name = getString();
    site.stmt = getStmt();
  }

  private BaseExpr getStmt()
  {
    String stmt = getString();
    if (stmt == null)
    {
      return null;
    }

    final BaseExpr result = new DummyStmt(stmt);
    BaseExpr next = result;
    for (stmt = getString(); stmt != null; stmt = getString())
    {
      next.next = new DummyStmt(stmt);
      next = next.next;
    }

    return result;
  }

  @Override
  public String toString()
  {
    return toStringRead();
  }
}
