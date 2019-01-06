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
import org.iota.qupla.helper.BaseContext;
import org.iota.qupla.helper.Verilog;

public class AbraToVerilogContext extends AbraBaseContext
{
  public ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
  private final Verilog verilog = new Verilog();

  private BaseContext appendVector(final String trits)
  {
    return verilog.appendVector(this, trits);
  }

  @Override
  public void eval(final AbraModule module)
  {
    fileOpen("AbraVerilog.txt");

    super.eval(module);

    verilog.addMergeLut(this);
    verilog.addMergeFuncs(this);

    fileClose();
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.specialType == AbraBaseBlock.TYPE_SLICE)
    {
      return;
    }

    newline();

    branchSites.clear();
    branchSites.addAll(branch.inputs);
    branchSites.addAll(branch.sites);
    branchSites.addAll(branch.outputs);
    branchSites.addAll(branch.latches);

    // give unnamed sites a name
    for (int i = 0; i < branchSites.size(); i++)
    {
      final AbraBaseSite site = branchSites.get(i);
      if (site.varName == null)
      {
        site.varName = "v_" + i;
      }
    }

    final String funcName = branch.name;
    append("function [" + (branch.size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent();

    boolean first = true;
    for (final AbraBaseSite input : branch.inputs)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input [" + (input.size * 2 - 1) + ":0] ").append(input.varName).newline();
    }

    append(");").newline();

    for (final AbraBaseSite site : branch.sites)
    {
      append("reg [" + (site.size * 2 - 1) + ":0] ").append(site.varName).append(";").newline();
    }

    if (branch.sites.size() != 0)
    {
      newline();
    }

    //TODO latches

    append("begin").newline().indent();

    for (final AbraBaseSite site : branch.sites)
    {
      if (site.references == 0)
      {
        continue;
      }

      append(site.varName).append(" = ");
      site.eval(this);
      append(";").newline();
    }

    append(funcName).append(" = ");
    if (branch.outputs.size() != 1)
    {
      append("{ ");
    }

    first = true;
    for (final AbraBaseSite output : branch.outputs)
    {
      append(first ? "" : ", ");
      first = false;
      output.eval(this);
    }

    if (branch.outputs.size() != 1)
    {
      append(" }");
    }

    append(";").newline();

    undent().append("end").newline().undent();
    append("endfunction").newline();

    //TODO branch.latches!
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    if (knot.block.specialType == AbraBaseBlock.TYPE_SLICE)
    {
      evalKnotSlice(knot);
      return;
    }

    append(knot.block.name);

    AbraBlockBranch branch = null;
    if (knot.block instanceof AbraBlockLut)
    {
      append("_lut");
    }
    else
    {
      branch = (AbraBlockBranch) knot.block;
    }

    boolean first = true;
    for (int i = 0; i < knot.inputs.size(); i++)
    {
      final AbraBaseSite input = knot.inputs.get(i);
      append(first ? "(" : ", ").append(input.varName);
      first = false;

      if (branch == null)
      {
        continue;
      }

      final AbraBaseSite param = branch.inputs.get(i);
      if (input.size > param.size)
      {
        // must take slice
        append("[" + param.size + ":0]");
      }
    }

    append(")");
  }

  private void evalKnotSlice(final AbraSiteKnot knot)
  {
    if (knot.inputs.size() > 1)
    {
      append("{ ");
    }

    int totalSize = 0;
    boolean first = true;
    for (final AbraBaseSite input : knot.inputs)
    {
      append(first ? "" : " : ").append(input.varName);
      first = false;
      totalSize += input.size;
    }

    if (knot.inputs.size() > 1)
    {
      append(" }");
    }

    final AbraBlockBranch branch = (AbraBlockBranch) knot.block;
    final AbraSiteParam input = (AbraSiteParam) branch.inputs.get(branch.inputs.size() - 1);
    if (totalSize > input.size)
    {
      final int start = input.offset * 2;
      final int end = start + input.size * 2 - 1;
      append("[" + end + ":" + start + "]");
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    final String lutName = lut.name + "_lut";
    append("function [1:0] ").append(lutName).append("(").newline().indent();

    boolean first = true;
    for (int i = 0; i < 3; i++)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input [1:0] ").append("p" + i).newline();
    }
    append(");").newline();

    append("begin").newline().indent();

    append("case ({p0, p1, p2})").newline();

    for (int i = 0; i < 27; i++)
    {
      char trit = lut.lookup.charAt(i);
      if (trit == '@')
      {
        continue;
      }

      appendVector(lutIndexes[i]).append(": ").append(lutName).append(" = ");
      appendVector("" + trit).append(";").newline();
    }

    append("default: ").append(lutName).append(" = ");
    appendVector("@").append(";").newline();
    append("endcase").newline().undent();

    append("end").newline().undent();
    append("endfunction").newline().newline();
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    if (merge.inputs.size() == 1)
    {
      // single-input merge just returns value
      final AbraBaseSite input = merge.inputs.get(0);
      append(input.varName);
      return;
    }

    verilog.mergefuncs.add(merge.size);

    for (int i = 0; i < merge.inputs.size() - 1; i++)
    {
      final AbraBaseSite input = merge.inputs.get(i);
      append(verilog.prefix + merge.size + "(").append(input.varName).append(", ");
    }

    final AbraBaseSite input = merge.inputs.get(merge.inputs.size() - 1);
    append(input.varName);

    for (int i = 0; i < merge.inputs.size() - 1; i++)
    {
      append(")");
    }
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
  }
}
