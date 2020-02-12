package org.iota.qupla.abra.context;

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
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.helper.DummyStmt;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraPrintContext extends AbraBaseContext
{
  public String fileName;
  public boolean statements = true;
  public String type = "site ";

  public AbraPrintContext(final String fileName)
  {
    this.fileName = fileName;
  }

  private void appendStmt(BaseExpr stmt)
  {
    while (stmt != null && statements)
    {
      newline();
      final String prefix = stmt instanceof AssignExpr || stmt instanceof DummyStmt ? "" : "return ";
      append(prefix + stmt).newline();
      stmt = stmt.next;
    }
  }

  @Override
  protected void appendify(final String text)
  {
    if (out == null && string == null)
    {
      System.out.print(text);
      return;
    }

    super.appendify(text);
  }

  private int depth(final AbraSiteKnot knot)
  {
    int maxDepth = 0;
    for (final AbraBaseSite input : knot.inputs)
    {
      if (input instanceof AbraSiteKnot)
      {
        final int thisDepth = depth((AbraSiteKnot) input) + 1;
        if (thisDepth > maxDepth)
        {
          maxDepth = thisDepth;
        }
      }
    }

    return maxDepth;
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

    append("## branch " + branch.name).newline().indent();

    evalBranchSites(branch.inputs, "in ");
    evalBranchSites(branch.latches, "latch ");
    evalBranchSites(branch.sites, "");

    appendStmt(branch.finalStmt);
    final String size = (branch.size < 10 ? " " : "") + branch.size;
    append("## 0  [" + size + "] out ");
    boolean first = true;
    for (final AbraBaseSite output : branch.outputs)
    {
      append(first ? "" : ", ");
      first = false;
      append(output.varName());
    }

    newline().undent();
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

    append("## import " + imp.toString());
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    evalSite(knot, depth(knot));

    final boolean isLut = knot.block instanceof AbraBlockLut;
    final String braces = isLut ? "[]" : knot.block.index == 0 ? "{}" : "()";

    append(" = ").append(knot.block.name).append(braces.substring(0, 1));

    boolean first = true;
    for (AbraBaseSite input : knot.inputs)
    {
      append(first ? "" : ", ");
      first = false;
      append(input.varName());
    }

    append(braces.substring(1, 2));
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch, 0);

    append(" = latch " + latch.name + "[" + latch.size + "]");
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    append("## lut " + lut.lookup + " " + lut.name).newline();
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param, 0);
  }

  private void evalSite(final AbraBaseSite site, final int level)
  {
    appendStmt(site.stmt);

    for (int i = 0; i < level; i++)
    {
      append(i < 10 ? "" + i + i : "##");
    }

    final String size = (site.size < 10 ? " " : "") + site.size;
    append("## " + site.references + "  [" + size + "] " + type + site.varName());
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
  }
}
