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
import org.iota.qupla.helper.DummyStmt;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraPrintContext extends AbraBaseContext
{
  public String fileName = "Abra.txt";
  public boolean statements = true;
  public String type = "site ";

  private void appendSiteInput(final AbraBaseSite input)
  {
    append(input.varName == null ? "p" + input.index : input.varName);
  }

  private void appendSiteInputs(final String braces, final AbraSiteMerge merge)
  {
    append(braces.substring(0, 1));

    boolean first = true;
    for (AbraBaseSite input : merge.inputs)
    {
      append(first ? "" : ", ");
      first = false;
      appendSiteInput(input);
    }

    append(braces.substring(1, 2));
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

    append("// branch " + branch.name);
    switch (branch.specialType)
    {
    case AbraBaseBlock.TYPE_CONSTANT:
      append(" // C");
      break;

    case AbraBaseBlock.TYPE_NULLIFY_FALSE:
      append(" // F");
      break;

    case AbraBaseBlock.TYPE_NULLIFY_TRUE:
      append(" // T");
      break;

    case AbraBaseBlock.TYPE_SLICE:
      append(" // S");
      break;

    case AbraBaseBlock.TYPE_MERGE:
      append(" // M");
      break;
    }

    newline().indent();

    evalBranchSites(branch.inputs, "in ");
    evalBranchSites(branch.sites, "");
    evalBranchSites(branch.outputs, "out ");
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

    append(" = ").append(knot.block.name);
    final String braces = (knot.block instanceof AbraBlockLut) ? "[]" : "()";
    appendSiteInputs(braces, knot);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch);

    append(" = latch " + latch.name + "[" + latch.size + "]");
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    append("// lut " + lut.lookup + " " + lut.name).newline();
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    evalSite(merge);

    if (merge.inputs.size() == 1)
    {
      append(" = ");
      appendSiteInput(merge.inputs.get(0));
      return;
    }

    append(" = merge");
    appendSiteInputs("{}", merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param);
  }

  private void evalSite(final AbraBaseSite site)
  {
    for (BaseExpr stmt = site.stmt; stmt != null && statements; stmt = stmt.next)
    {
      newline();
      final String prefix = stmt instanceof AssignExpr || stmt instanceof DummyStmt ? "" : "return ";
      append(prefix + stmt).newline();
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

    final String name = site.varName == null ? "p" + site.index : site.varName;
    final String size = (site.size < 10 ? " " : "") + site.size;
    append("// " + site.references + nullifyIndex);
    append(" " + "[" + size + "] " + type + name);
  }
}
