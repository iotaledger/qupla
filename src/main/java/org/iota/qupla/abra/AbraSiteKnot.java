package org.iota.qupla.abra;

import java.util.ArrayList;
import java.util.HashMap;

import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraSiteKnot extends AbraSite
{
  public static int concatReuse;
  public static HashMap<String, AbraBlockBranch> concats = new HashMap<>();
  public static int sliceReuse;
  public static HashMap<String, AbraBlockBranch> slicers = new HashMap<>();
  public static int vectorReuse;
  public static HashMap<String, AbraBlockBranch> vectors = new HashMap<>();
  public AbraBlock block;
  public ArrayList<AbraSite> inputs = new ArrayList<>();

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).append("knot(");
    boolean first = true;
    for (AbraSite input : inputs)
    {
      context.append(first ? "" : ", ").append("" + refer(input.index));
      first = false;
    }

    return context.append(") " + block);
  }

  public void branch(final AbraContext context)
  {
    for (final AbraBlock branch : context.abra.branches)
    {
      if (branch.name.equals(name))
      {
        block = branch;
        break;
      }
    }
  }

  @Override
  public void code(final TritCode tritCode)
  {
    tritCode.putTrit('-');
    tritCode.putInt(inputs.size());
    for (final AbraSite input : inputs)
    {
      tritCode.putInt(refer(input.index));
    }

    tritCode.putInt(block.index);
  }

  public void concat(final AbraContext context)
  {
    final String branchName = "$concat$" + size;
    block = concats.get(branchName);
    if (block != null)
    {
      concatReuse++;
      return;
    }

    // generate block that does concatenation
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = branchName;
    branch.size = size;

    final AbraSiteParam targetSite = new AbraSiteParam();
    targetSite.size = size;
    targetSite.index = branch.inputs.size();
    targetSite.name = "P" + targetSite.index;
    branch.inputs.add(targetSite);

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.inputs.add(targetSite);
    merge.size = size;
    merge.index = branch.inputs.size();
    branch.sites.add(merge);
    branch.outputs.add(merge);

    context.abra.branches.add(branch);
    concats.put(branchName, branch);
    block = branch;
  }

  public void lut(final AbraContext context)
  {
    for (final AbraBlock lut : context.abra.luts)
    {
      if (lut.name.equals(name))
      {
        block = lut;
        break;
      }
    }
  }

  public void slice(final AbraContext context, final int start)
  {
    final int inputSize = inputs.get(0).size;
    final String branchName = "$slice$" + inputSize + "$" + start + "$" + size;
    block = slicers.get(branchName);
    if (block != null)
    {
      sliceReuse++;
      return;
    }

    // generate block that does slicing
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = branchName;
    branch.size = size;

    if (start > 0)
    {
      final AbraSiteParam site = new AbraSiteParam();
      site.size = start;
      site.index = branch.inputs.size();
      site.name = "P" + site.index;
      branch.inputs.add(site);
    }

    final AbraSiteParam targetSite = new AbraSiteParam();
    targetSite.size = size;
    targetSite.index = branch.inputs.size();
    targetSite.name = "P" + targetSite.index;
    branch.inputs.add(targetSite);

    if (start + size < inputSize)
    {
      final AbraSiteParam site = new AbraSiteParam();
      site.size = inputSize - start - size;
      site.index = branch.inputs.size();
      site.name = "P" + site.index;
      branch.inputs.add(site);
    }

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.inputs.add(targetSite);
    merge.size = size;
    merge.index = branch.inputs.size();
    branch.sites.add(merge);
    branch.outputs.add(merge);

    context.abra.branches.add(branch);
    slicers.put(branchName, branch);
    block = branch;
  }

  public void vector(final AbraContext context, final String trits)
  {
    final String branchName = "const$" + size + "$" + name;
    block = vectors.get(branchName);
    if (block != null)
    {
      vectorReuse++;
      return;
    }

    // create branch that has 1 input of 1 trit
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = branchName;
    branch.size = size;

    final AbraSiteParam param = new AbraSiteParam();
    param.index = 0;
    param.name = "dummy";
    param.size = 1;
    branch.inputs.add(param);

    final AbraSiteKnot siteMin = vectorTritLut(context, branch, "constMin$0");
    final AbraSiteKnot siteOne = vectorTritLut(context, branch, "constOne$0");
    final AbraSiteKnot siteZero = vectorTritLut(context, branch, "constZero$0");

    final AbraSiteKnot constant = new AbraSiteKnot();
    constant.index = 4;
    constant.size = branch.size;
    for (int i = 0; i < branch.size; i++)
    {
      final char c = trits.charAt(i);
      constant.inputs.add(c == '0' ? siteZero : c == '1' ? siteOne : siteMin);
    }

    constant.concat(context);
    branch.sites.add(constant);
    branch.outputs.add(constant);

    context.abra.branches.add(branch);
    vectors.put(branchName, branch);
    block = branch;
  }

  public AbraSiteKnot vectorTritLut(final AbraContext context, final AbraBlockBranch branch, final String lutName)
  {
    final AbraSiteKnot site = new AbraSiteKnot();
    site.index = branch.sites.size() + 1;
    site.name = lutName;
    site.size = 1;
    site.inputs.add(branch.inputs.get(0));
    site.lut(context);
    branch.sites.add(site);
    return site;
  }
}
