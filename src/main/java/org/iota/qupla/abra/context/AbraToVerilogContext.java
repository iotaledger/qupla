package org.iota.qupla.abra.context;

import java.util.ArrayList;
import java.util.HashSet;

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

public class AbraToVerilogContext extends AbraBaseContext
{
  public ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
  public HashSet<Integer> mergefuncs = new HashSet<>();

  public AbraToVerilogContext()
  {
  }

  private void addMergeFuncs()
  {
    newline().append("x reg [0:0];").newline();
    newline().append("function [1:0] Qupla_merge(").newline().indent();
    append("  input [1:0] input1").newline();
    append(", input [1:0] input2").newline();
    append(");").newline();
    append("begin").newline().indent();
    append("case ({input1, input2})").newline();
    append("4'b0000: Qupla_merge = 2'b00;").newline();
    append("4'b0001: Qupla_merge = 2'b01;").newline();
    append("4'b0010: Qupla_merge = 2'b10;").newline();
    append("4'b0011: Qupla_merge = 2'b11;").newline();
    append("4'b0100: Qupla_merge = 2'b01;").newline();
    append("4'b1000: Qupla_merge = 2'b10;").newline();
    append("4'b1100: Qupla_merge = 2'b11;").newline();
    append("4'b0101: Qupla_merge = 2'b01;").newline();
    append("4'b1010: Qupla_merge = 2'b10;").newline();
    append("4'b1111: Qupla_merge = 2'b11;").newline();
    append("default: Qupla_merge = 2'b00;").newline();
    append("         x <= 1;").newline();
    append("endcase").newline().undent();
    append("end").newline().undent();
    append("endfunction").newline();

    for (final Integer size : mergefuncs)
    {
      final String funcName = "Qupla_merge_" + size;
      newline().append("function [" + (size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent();
      append("  input [" + (size * 2 - 1) + ":0] input1").newline();
      append(", input [" + (size * 2 - 1) + ":0] input2").newline();
      append(");").newline();
      append("begin").newline().indent();
      append(funcName).append(" = {").newline().indent();
      boolean first = true;
      for (int i = 0; i < size; i++)
      {
        final int from = i * 2 + 1;
        final int to = i * 2;
        append(first ? "" : ": ").append("Qupla_merge(input1[" + from + ":" + to + "], input2[" + from + ":" + to + "])").newline();
        first = false;
      }
      undent();
      append("};").newline().undent();
      append("end").newline().undent();
      append("endfunction").newline();
    }
  }

  private AbraToVerilogContext appendVector(final String trits)
  {
    final int size = trits.length() * 2;
    append(size + "'b");
    for (int i = 0; i < trits.length(); i++)
    {
      switch (trits.charAt(i))
      {
      case '0':
        append("01");
        break;
      case '1':
        append("10");
        break;
      case '-':
        append("11");
        break;
      case '@':
        append("00");
        break;
      }
    }

    return this;
  }

  @Override
  public void eval(final AbraModule module)
  {
    fileOpen("AbraVerilog.txt");

    super.eval(module);

    addMergeFuncs();

    fileClose();
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.type == AbraBaseBlock.TYPE_SLICE)
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
    if (knot.block.type == AbraBaseBlock.TYPE_SLICE)
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

    mergefuncs.add(merge.size);

    for (int i = 0; i < merge.inputs.size() - 1; i++)
    {
      final AbraBaseSite input = merge.inputs.get(i);
      append(("Qupla_merge_" + merge.size) + "(").append(input.varName).append(", ");
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
