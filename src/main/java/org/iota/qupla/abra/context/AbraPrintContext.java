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

public class AbraPrintContext extends AbraBaseContext
{
  public void appendSiteInputs(final AbraSiteMerge merge)
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

    fileOpen("Abra.txt");

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

    append("// " + block.toString());
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    branch.numberSites();

    evalBlock(branch);

    newline().indent();

    evalBranchSites(branch.inputs, "input");
    evalBranchSites(branch.sites, "body");
    evalBranchSites(branch.outputs, "output");
    evalBranchSites(branch.latches, "latch");

    undent();
  }

  private void evalBranchSites(final ArrayList<? extends AbraBaseSite> sites, final String type)
  {
    for (final AbraBaseSite site : sites)
    {
      site.type = type;
      site.eval(this);
      newline();
    }
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    evalBlock(imp);
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
    evalBlock(lut);

    append("// lut block " + lut.index);
    append(" // " + lut.lookup);
    append(" // " + lut.name + "[]").newline();
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
    if (site.stmt != null)
    {
      newline().append("" + site.stmt).newline();
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

    append("// " + site.index + " ").append(nullifyIndex);
    append(" " + site.references + " " + site.type + " site(" + site.size + "): ");
  }
}
