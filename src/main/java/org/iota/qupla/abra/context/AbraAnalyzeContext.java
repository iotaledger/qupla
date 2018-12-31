package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockImport;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteLatch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;

public class AbraAnalyzeContext extends AbraCodeContext
{
  private static final String constMin = "---------------------------";
  private static final String constOne = "111111111111111111111111111";
  private static final String constZero = "000000000000000000000000000";
  private static final String nullifyFalse = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
  private static final String nullifyTrue = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
  private static final boolean sanityCheck = true;
  public int missing;
  public int offset;

  public void check(final boolean condition)
  {
    if (sanityCheck && !condition)
    {
      error("Check failed");
    }
  }

  private void clearSizes(final ArrayList<AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.oldSize = site.size;
      site.size = 0;
    }
  }

  public void ensure(final boolean condition)
  {
    if (!condition)
    {
      error("Ensure failed");
    }
  }

  public void error(final String text)
  {
    throw new CodeException(null, text);
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    if (branch.analyzed)
    {
      return;
    }

    branch.analyzed = true;

    offset = 0;
    int index = 0;
    int lastMissing = missing;
    index = evalBranchSites(index, branch.inputs, "input");
    index = evalBranchSites(index, branch.sites, "body");
    index = evalBranchSites(index, branch.outputs, "output");
    index = evalBranchSites(index, branch.latches, "latch");

    int size = 0;
    for (final AbraSite output : branch.outputs)
    {
      if (output.size == 0)
      {
        // insufficient data to calculate return size yet
        branch.analyzed = false;
        return;
      }

      size += output.size;
    }

    branch.size = size;
    ensure(branch.size != 0);
    check(branch.oldSize == branch.size || branch.oldSize == 0);

    if (lastMissing != missing)
    {
      // come back later to fill in missing sites
      branch.analyzed = false;
      return;
    }

    evalBranchSpecial(branch);

    branch.analyzed = true;
  }

  public int evalBranchSites(int index, final ArrayList<AbraSite> sites, final String type)
  {
    for (final AbraSite site : sites)
    {
      check(site.index == index);
      site.index = index++;

      check(site.type.equals(type));
      site.type = type;

      site.eval(this);
    }

    return index;
  }

  private boolean evalBranchSpecial(final AbraBlockBranch branch)
  {
    if (branch.latches.size() != 0)
    {
      return false;
    }

    return evalBranchSpecialConstantZero(branch) || //
        evalBranchSpecialConstantNonZero(branch) || //
        evalBranchSpecialNullify(branch) || //
        evalBranchSpecialSlice(branch);
  }

  private boolean evalBranchSpecialConstantNonZero(final AbraBlockBranch branch)
  {
    // nonzero constant function has 1 input, multiple sites, and 1 output
    if (branch.inputs.size() != 1 || branch.sites.size() < 2 || branch.outputs.size() != 1)
    {
      return false;
    }

    // input is any single trit that triggers data flow
    final AbraSite inputTrit = branch.inputs.get(0);
    if (inputTrit.size != 1)
    {
      return false;
    }

    TritVector constant = null;
    for (final AbraSite site : branch.sites)
    {
      if (!(site instanceof AbraSiteKnot))
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) site;
      if (knot.block.type != AbraBlock.TYPE_CONSTANT)
      {
        return false;
      }

      // all inputs triggered by input trit
      for (final AbraSite input : knot.inputs)
      {
        if (input != inputTrit)
        {
          return false;
        }
      }

      constant = TritVector.concat(constant, knot.block.constantValue);
    }

    final AbraSite output = branch.outputs.get(0);
    if (!(output instanceof AbraSiteKnot))
    {
      return false;
    }

    final AbraSiteKnot knot = (AbraSiteKnot) output;
    if (knot.block.type != AbraBlock.TYPE_SLICE || knot.inputs.size() != branch.sites.size())
    {
      return false;
    }

    //TODO could verify that all knot.inputs are all branch.sites

    check(branch.name != null && branch.name.startsWith("const_"));

    branch.type = AbraBlock.TYPE_CONSTANT;
    branch.constantValue = constant.slice(0, knot.size);
    return true;
  }

  private boolean evalBranchSpecialConstantZero(final AbraBlockBranch branch)
  {
    if (branch.inputs.size() != 1 || branch.sites.size() != 0)
    {
      return false;
    }

    // input is any single trit that triggers data flow
    final AbraSite inputTrit = branch.inputs.get(0);
    if (inputTrit.size != 1)
    {
      return false;
    }

    TritVector constant = null;
    for (final AbraSite output : branch.outputs)
    {
      if (!(output instanceof AbraSiteKnot))
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) output;
      if (knot.block.type != AbraBlock.TYPE_CONSTANT)
      {
        return false;
      }

      // all inputs triggered by input trit
      for (final AbraSite input : knot.inputs)
      {
        if (input != inputTrit)
        {
          return false;
        }
      }

      constant = TritVector.concat(constant, knot.block.constantValue);
    }

    check(branch.name != null && branch.name.startsWith("constZero"));

    branch.type = AbraBlock.TYPE_CONSTANT;
    branch.constantValue = constant;
    return true;
  }

  private boolean evalBranchSpecialNullify(final AbraBlockBranch branch)
  {
    if (branch.sites.size() != 0 || branch.outputs.size() == 0)
    {
      return false;
    }

    // nullify function has 1 input more than outputs
    if (branch.inputs.size() != branch.outputs.size() + 1)
    {
      return false;
    }

    // first input is the boolean flag
    final AbraSite inputFlag = branch.inputs.get(0);
    if (inputFlag.size != 1)
    {
      return false;
    }

    final AbraSite firstOutput = branch.outputs.get(0);
    if (!(firstOutput instanceof AbraSiteKnot))
    {
      return false;
    }

    final int type = ((AbraSiteKnot) firstOutput).block.type;
    if (type != AbraBlock.TYPE_NULLIFY_FALSE && type != AbraBlock.TYPE_NULLIFY_TRUE)
    {
      return false;
    }

    for (int i = 0; i < branch.outputs.size(); i++)
    {
      final AbraSite output = branch.outputs.get(0);
      if (!(output instanceof AbraSiteKnot))
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) output;
      if (knot.block.type != type)
      {
        // must be same nullify type
        return false;
      }

      if (knot.inputs.get(0) != inputFlag || knot.inputs.get(1) != branch.inputs.get(i + 1))
      {
        return false;
      }

      //TODO double-check number of knot inputs (2 or 3) against knot type (branch or lut)?
    }

    check(branch.name != null && branch.name.startsWith("nullify"));

    // set nullify type and have a correctly sized null vector ready
    branch.type = type;
    branch.constantValue = new TritVector(branch.size, '@');
    return true;
  }

  private boolean evalBranchSpecialSlice(final AbraBlockBranch branch)
  {
    if (branch.inputs.size() > 2 || branch.sites.size() != 0 || branch.outputs.size() != 1)
    {
      return false;
    }

    // last input is the sliced input
    final AbraSiteParam input = (AbraSiteParam) branch.inputs.get(branch.inputs.size() - 1);

    final AbraSite output = branch.outputs.get(0);
    if (!(output instanceof AbraSiteMerge))
    {
      return false;
    }

    final AbraSiteMerge merge = (AbraSiteMerge) output;
    if (merge.inputs.size() != 1 || merge.inputs.get(0) != input)
    {
      return false;
    }

    branch.type = AbraBlock.TYPE_SLICE;
    branch.offset = input.offset;
    return true;
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {

  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    if (knot.inputs.size() == 0 && knot.references == 0)
    {
      return;
    }

    knot.block.eval(this);
    if (knot.block.size() == 0)
    {
      missing++;
      return;
    }

    knot.size = knot.block.size();
    ensure(knot.size != 0);
    check(knot.oldSize == knot.size || knot.oldSize == 0);

    if (knot.block instanceof AbraBlockBranch)
    {
      evalKnotBranch(knot);
      return;
    }

    // knot.block is a lut
    ensure(knot.inputs.size() == 3);

    for (final AbraSite input : knot.inputs)
    {
      ensure(input.size == 1);
    }
  }

  private void evalKnotBranch(final AbraSiteKnot knot)
  {
    //TODO
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    check(latch.references == 0);
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    if (lut.analyzed)
    {
      return;
    }

    lut.analyzed = true;

    final String trits = new String(lut.tritCode.buffer, 0, 27);

    if (trits.equals(constZero))
    {
      lut.type = AbraBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '0');
      return;
    }

    if (trits.equals(constMin))
    {
      lut.type = AbraBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '-');
      return;
    }

    if (trits.equals(constOne))
    {
      lut.type = AbraBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '1');
      return;
    }

    if (trits.equals(nullifyFalse))
    {
      lut.type = AbraBlock.TYPE_NULLIFY_FALSE;
      return;
    }

    if (trits.equals(nullifyTrue))
    {
      lut.type = AbraBlock.TYPE_NULLIFY_TRUE;
      return;
    }
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    if (merge.inputs.size() == 0 && merge.references == 0)
    {
      return;
    }

    for (final AbraSite input : merge.inputs)
    {
      if (input.size != 0)
      {
        if (merge.size == 0)
        {
          merge.size = input.size;
          continue;
        }

        if (merge.size != input.size)
        {
          error("Merge size mismatch");
        }
      }
    }

    if (merge.size == 0)
    {
      missing++;
      return;
    }

    check(merge.oldSize == merge.size || merge.oldSize == 0);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    check(param.offset == offset);

    ensure(param.size > 0);
    ensure(!param.isLatch);
    ensure(param.nullifyFalse == null);
    ensure(param.nullifyTrue == null);

    param.offset = offset;
    offset += param.size;
  }

  @Override
  public void finished()
  {
    int lastMissing = 0;
    while (missing != lastMissing)
    {
      lastMissing = missing;
      missing = 0;
      for (final AbraBlockBranch branch : abraCode.branches)
      {
        branch.eval(this);
      }
    }

    if (missing != 0)
    {
      for (final AbraBlockBranch branch : abraCode.branches)
      {
        if (branch.size() == 0)
        {
          BaseExpr.logLine("Unresolved trit vector size in branch: " + branch.name);
        }
      }

      error("Recursion issue detected");
    }

    // quick sanity check if everything has a size now
    for (final AbraBlockBranch branch : abraCode.branches)
    {
      for (final AbraSite site : branch.sites)
      {
        if (site.size == 0 && ((AbraSiteMerge) site).inputs.size() != 0)
        {
          error("WTF?");
        }
      }

      for (final AbraSite site : branch.outputs)
      {
        if (site.size == 0 && ((AbraSiteMerge) site).inputs.size() != 0)
        {
          error("WTF?");
        }
      }

      for (final AbraSite site : branch.latches)
      {
        if (site.size == 0 && site.references != 0)
        {
          error("WTF?");
        }
      }
    }
  }

  @Override
  public void started()
  {
    // we're going to recalculate all sizes
    for (final AbraBlockBranch branch : abraCode.branches)
    {
      branch.oldSize = branch.size;
      branch.size = 0;
      clearSizes(branch.sites);
      clearSizes(branch.outputs);
      clearSizes(branch.latches);
    }
  }
}
