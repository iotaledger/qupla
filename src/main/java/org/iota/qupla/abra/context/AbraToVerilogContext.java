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
import org.iota.qupla.helper.BaseContext;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.Verilog;

//TODO correct handling of merge and nullify (implement nullify functions)
public class AbraToVerilogContext extends AbraBaseContext
{
  private final static int[] lutSize = {
      1,
      3,
      9,
      27
  };
  private final ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
  private final Verilog verilog = new Verilog();

  private BaseContext appendName(final AbraBaseSite site)
  {
    append(site.name == null ? "v_" + site.index : site.name);
    return this;
  }

  private BaseContext appendVector(final String trits)
  {
    append(verilog.vector(trits));
    return this;
  }

  @Override
  public void eval(final AbraModule module)
  {
    fileOpen("AbraVerilog.txt");

    super.eval(module);

    verilog.generateMergeFuncs(this);

    fileClose();
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.index == AbraBlockSpecial.TYPE_SLICE)
    {
      return;
    }

    newline();

    branchSites.clear();
    branchSites.addAll(branch.inputs);
    branchSites.addAll(branch.latches);
    branchSites.addAll(branch.sites);

    final String funcName = branch.name;
    append("function ").append(size(branch.size)).append(" ").append(funcName).append("(").newline().indent();

    boolean first = true;
    for (final AbraBaseSite input : branch.inputs)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input ").append(size(input.size)).append(" ");
      appendName(input).newline();
    }

    append(");").newline();

    //TODO branch.latches

    for (final AbraSiteKnot site : branch.sites)
    {
      append("reg ").append(size(site.size)).append(" ");
      appendName(site).append(";").newline();
    }

    if (branch.sites.size() != 0)
    {
      newline();
    }

    append("begin").newline().indent();

    for (final AbraSiteKnot site : branch.sites)
    {
      if (site.references == 0)
      {
        continue;
      }

      appendName(site).append(" = ");
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
      appendName(output);
    }

    if (branch.outputs.size() != 1)
    {
      append(" }");
    }

    append(";").newline();

    //TODO branch.latches!

    undent().append("end").newline().undent();
    append("endfunction").newline();
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
  }

  @Override
  protected void evalKnotBranch(final AbraSiteKnot knot, final AbraBlockBranch block)
  {
    append(knot.block.name);

    if (block == null)
    {
      append("_lut");
    }

    boolean first = true;
    for (int i = 0; i < knot.inputs.size(); i++)
    {
      final AbraBaseSite input = knot.inputs.get(i);
      append(first ? "(" : ", ");
      appendName(input);
      first = false;

      if (block == null)
      {
        continue;
      }

      final AbraBaseSite param = block.inputs.get(i);
      if (input.size > param.size)
      {
        // must take slice
        append(size(param.size));
      }
    }

    append(")");
  }

  @Override
  protected void evalKnotLut(final AbraSiteKnot knot, final AbraBlockLut block)
  {
    evalKnotBranch(knot, null);
  }

  @Override
  protected void evalKnotSpecial(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    switch (block.index)
    {
    case AbraBlockSpecial.TYPE_CONCAT:
    case AbraBlockSpecial.TYPE_SLICE:
      evalKnotSpecialSlice(knot, block);
      break;

    case AbraBlockSpecial.TYPE_CONST:
      appendVector(block.constantValue.trits());
      break;

    case AbraBlockSpecial.TYPE_MERGE:
      evalKnotSpecialMerge(knot);
      break;

    case AbraBlockSpecial.TYPE_NULLIFY_FALSE:
    case AbraBlockSpecial.TYPE_NULLIFY_TRUE:
      evalKnotSpecialNullify(knot);
      break;
    }
  }

  private void evalKnotSpecialMerge(final AbraSiteKnot merge)
  {
    if (merge.inputs.size() == 1)
    {
      // single-input merge just returns value
      final AbraBaseSite input = merge.inputs.get(0);
      appendName(input);
      return;
    }

    verilog.mergeFuncs.add(merge.size);

    for (int i = 0; i < merge.inputs.size() - 1; i++)
    {
      final AbraBaseSite input = merge.inputs.get(i);
      append(verilog.prefix + merge.size + "(");
      appendName(input).append(", ");
    }

    final AbraBaseSite input = merge.inputs.get(merge.inputs.size() - 1);
    appendName(input);

    for (int i = 0; i < merge.inputs.size() - 1; i++)
    {
      append(")");
    }
  }

  private void evalKnotSpecialNullify(final AbraSiteKnot knot)
  {
  }

  private void evalKnotSpecialSlice(final AbraSiteKnot knot, final AbraBlockSpecial block)
  {
    if (knot.inputs.size() > 1)
    {
      append("{ ");
    }

    int totalSize = 0;
    boolean first = true;
    for (final AbraBaseSite input : knot.inputs)
    {
      append(first ? "" : ", ");
      appendName(input);
      first = false;
      totalSize += input.size;
    }

    if (knot.inputs.size() > 1)
    {
      append(" }");
    }

    if (totalSize > block.size)
    {
      append(verilog.range(totalSize - block.offset, block.size));
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    final int inputSize = lut.inputs();
    //    for (int i = 0; i < lutSize[inputSize]; i++)
    //    {
    //      char trit = lut.lookup.charAt(i);
    //      append("// ");
    //      final String input = TritConverter.TRYTE_TRITS[i].substring(0, inputSize);
    //      append(verilog.vector(input).substring(3)).append(": ");
    //      append(verilog.vector("" + trit).substring(3)).newline();
    //    }

    final String code = verilog.file(lut.name);
    if (code != null)
    {
      append(code).newline();
      return;
    }

    final String lutName = lut.name + "_lut";
    append("function ").append(size(1)).append(" ").append(lutName).append("(").newline().indent();

    boolean first = true;
    for (int i = 0; i < inputSize; i++)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input ").append(size(1)).append(" p" + i).newline();
    }
    append(");").newline();

    append("begin").newline().indent();

    append("case ({");
    first = true;
    for (int i = 0; i < inputSize; i++)
    {
      append(first ? "" : ", ").append("p" + i);
      first = false;
    }

    append("})").newline();

    for (int i = 0; i < lutSize[inputSize]; i++)
    {
      char trit = lut.lookup.charAt(i);
      if (trit == '@')
      {
        continue;
      }

      appendVector(TritConverter.TRYTE_TRITS[i].substring(0, inputSize)).append(": ").append(lutName).append(" = ");
      appendVector("" + trit).append(";").newline();
    }

    append("default: ").append(lutName).append(" = ");
    appendVector("@").append(";").newline();
    append("endcase").newline().undent();

    append("end").newline().undent();
    append("endfunction").newline().newline();
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
  }

  @Override
  public void evalSpecial(final AbraBlockSpecial block)
  {
  }

  private String size(final int trits)
  {
    return verilog.size(trits);
  }
}
