package org.iota.qupla.abra.context;

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
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraPrintContext extends AbraBaseContext
{
  public String fileName = "Abra.txt";
  public boolean statements = true;
  public String type = "site";

  private void appendSiteInputs(final AbraSiteMerge merge)
  {
    append("(");

    boolean first = true;
    for (AbraBaseSite input : merge.inputs)
    {
      append(first ? "" : ", ").append("" + input.index);
      first = false;
    }

    append(")");
  }

  public void eval(final AbraModule module)
  {
    module.blockNr = 0;
    module.numberBlocks(module.blocks);

    fileOpen(fileName);

    evalBlocks(module.imports);
    evalBlocks(module.luts);
    evalBlocks(module.branches);

    fileClose();
  }

  private void evalBlock(final AbraBaseBlock block)
  {
    newline();
    if (block.origin != null)
    {
      append("" + block.origin).newline();
    }
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    branch.numberSites();

    evalBlock(branch);

    append("// branch " + branch.toString());

    newline().indent();

    evalBranchSites(branch.inputs, "input ");
    evalBranchSites(branch.sites, "body  ");
    evalBranchSites(branch.outputs, "output");
    evalBranchSites(branch.latches, "latch ");

    undent();
  }

  private void evalBranchSites(final ArrayList<? extends AbraBaseSite> sites, final String type)
  {
    this.type = type;
    for (final AbraBaseSite site : sites)
    {
      site.eval(this);
      newline();
    }
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    evalBlock(imp);

    append("// import " + imp.toString());
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    evalSite(knot);

    append("knot");
    appendSiteInputs(knot);
    append(" " + knot.block);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch);

    append("latch " + latch.name + "[" + latch.size + "]");
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    append("// lut " + lut.lookup + " " + lut.toString()).newline();
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    evalSite(merge);

    append("merge");
    appendSiteInputs(merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param);

    append("param " + param.name + "[" + param.size + "]");
  }

  private void evalSite(final AbraBaseSite site)
  {
    for (BaseExpr stmt = site.stmt; stmt != null && statements; stmt = stmt.next)
    {
      newline().append(stmt.toString()).newline();
    }

    String nullifyIndex = " ";
    if (site.nullifyTrue != null)
    {
      nullifyIndex = "T" + site.nullifyTrue.index;
    }

    if (site.nullifyFalse != null)
    {
      nullifyIndex = "F" + site.nullifyFalse.index;
    }

    final String index = (site.index < 10 ? " " : "") + site.index;
    final String size = (site.size < 10 ? " " : "") + site.size;
    append("// " + index + " ").append(nullifyIndex);
    append(" " + site.references + " " + type + "[" + size + "] ");
  }
}
