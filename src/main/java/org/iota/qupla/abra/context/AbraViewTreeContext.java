package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;

public class AbraViewTreeContext extends AbraBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    final boolean oldStatements = AbraBaseSite.printer.statements;
    AbraBaseSite.printer.statements = false;
    fileOpen("AbraTree.txt");
    super.eval(module);
    fileClose();
    AbraBaseSite.printer.statements = oldStatements;
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    newline().append("evalBranch: " + branch.name).newline();
    indent();

    evalBranchSites(branch.inputs, "input");
    evalBranchSites(branch.latches, "latch");
    evalBranchSites(branch.sites, "body");

    boolean first = true;
    for (final AbraBaseSite output : branch.outputs)
    {
      append(first ? "output sites: " : ", ").append(output.varName());
      first = false;
    }
    newline();

    undent();
  }

  private void evalBranchSites(final ArrayList<? extends AbraBaseSite> sites, final String type)
  {
    if (sites.size() == 0)
    {
      return;
    }

    append(type + " sites:").newline();
    indent();
    for (final AbraBaseSite site : sites)
    {
      site.eval(this);
    }

    undent();
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    append("evalImport: " + imp).newline();
  }

  @Override
  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    append("evalKnotBranch:  " + knot).newline();
  }

  @Override
  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
    append("evalKnotLut:     " + knot).newline();
  }

  @Override
  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    append("evalKnotSpecial: " + knot).newline();
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    append("evalLatch: " + latch).newline();
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    append("evalLut: " + lut.name).newline();
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    append("evalParam: " + param).newline();
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
    append("evalSpecial: " + block.name).newline();
  }
}
