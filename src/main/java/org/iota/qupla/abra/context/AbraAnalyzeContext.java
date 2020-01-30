package org.iota.qupla.abra.context;

import java.util.ArrayList;

import org.iota.qupla.Qupla;
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
import org.iota.qupla.abra.funcmanagers.ConstFuncManager;
import org.iota.qupla.abra.funcmanagers.ConstZeroFuncManager;
import org.iota.qupla.abra.funcmanagers.NullifyFuncManager;
import org.iota.qupla.helper.TritVector;

public class AbraAnalyzeContext extends AbraBaseContext
{
  private static final boolean sanityCheck = true;
  public int missing;
  public int offset;

  private void check(final boolean condition)
  {
    if (sanityCheck && !condition)
    {
      error("Check failed");
    }
  }

  private void clearSizes(final ArrayList<AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.size = 0;
    }
  }

  private void clearSizes(final AbraModule module)
  {
    for (final AbraBlockBranch branch : module.branches)
    {
      branch.size = 0;
      clearSizes(branch.sites);
      clearSizes(branch.outputs);
      clearSizes(branch.latches);
      for (final AbraBaseSite latch : branch.latches)
      {
        latch.isLatch = true;
      }
    }
  }

  private void ensure(final boolean condition)
  {
    if (!condition)
    {
      error("Ensure failed");
    }
  }

  @Override
  public void eval(final AbraModule module)
  {
    // we're going to recalculate all sizes
    clearSizes(module);

    // single pass over everything
    super.eval(module);

    // some sizes may have been indeterminable for now due to recursion
    resolveRecursions(module);
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
    index = evalBranchSites(index, branch.inputs);
    index = evalBranchSites(index, branch.sites);
    index = evalBranchSites(index, branch.outputs);
    index = evalBranchSites(index, branch.latches);

    int size = 0;
    for (final AbraBaseSite output : branch.outputs)
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

    if (lastMissing != missing)
    {
      // come back later to fill in missing sites
      branch.analyzed = false;
      return;
    }

    evalBranchSpecial(branch);

    branch.analyzed = true;
  }

  private int evalBranchSites(int index, final ArrayList<AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      check(site.index == index);
      site.index = index++;

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

    final boolean special = //
        evalBranchSpecialConstant(branch) || //
            evalBranchSpecialMerge(branch) || //
            evalBranchSpecialNullify(branch) || //
            evalBranchSpecialSlice(branch);

    return special;
  }

  private boolean evalBranchSpecialConstant(final AbraBlockBranch branch)
  {
    if (branch.inputs.size() != 1)
    {
      // nonzero constant function has 1 input
      return false;
    }

    final AbraBaseSite singleTrit = branch.inputs.get(0);
    if (singleTrit.size != 1)
    {
      // input must be a single trit
      return false;
    }

    for (final AbraBaseSite site : branch.sites)
    {
      if (!(site instanceof AbraSiteKnot))
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) site;
      if (knot.block.specialType != AbraBaseBlock.TYPE_CONSTANT)
      {
        return false;
      }

      // all inputs triggered by input trit
      for (final AbraBaseSite input : knot.inputs)
      {
        if (input != singleTrit)
        {
          return false;
        }
      }
    }

    TritVector constant = null;
    for (final AbraBaseSite site : branch.outputs)
    {
      if (site instanceof AbraSiteKnot)
      {
        final AbraSiteKnot knot = (AbraSiteKnot) site;
        if (knot.block.specialType != AbraBaseBlock.TYPE_CONSTANT)
        {
          return false;
        }

        // all inputs triggered by input trit
        for (final AbraBaseSite input : knot.inputs)
        {
          if (input != singleTrit)
          {
            return false;
          }
        }
        constant = TritVector.concat(constant, knot.block.constantValue);
        continue;
      }

      if (site instanceof AbraSiteMerge)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) site;
        if (merge.inputs.size() != 1)
        {
          return false;
        }

        final AbraBaseSite input = merge.inputs.get(0);
        if (!branch.sites.contains(input))
        {
          return false;
        }

        constant = TritVector.concat(constant, ((AbraSiteKnot) input).block.constantValue);
        continue;
      }

      return false;
    }

    final String prefix = constant.isZero() ? "constZero" : "const_";
    check(branch.name != null && branch.name.startsWith(prefix));

    branch.constantValue = constant;
    branch.specialType = AbraBaseBlock.TYPE_CONSTANT;
    return true;
  }

  private boolean evalBranchSpecialMerge(final AbraBlockBranch branch)
  {
    if ((branch.inputs.size() & 1) == 1 || branch.sites.size() != branch.outputs.size())
    {
      return false;
    }

    for (int i = 0; i < branch.sites.size(); i++)
    {
      final AbraBaseSite input1 = branch.inputs.get(i);
      final AbraBaseSite input2 = branch.inputs.get(i + branch.outputs.size());
      final AbraBaseSite site = branch.sites.get(i);
      final AbraBaseSite output = branch.outputs.get(i);
      if (!(site instanceof AbraSiteMerge) || !(output instanceof AbraSiteMerge))
      {
        return false;
      }

      final AbraSiteMerge out = (AbraSiteMerge) output;
      if (out.inputs.size() != 1 || out.inputs.get(0) != site)
      {
        return false;
      }

      if (input1.size == 1 && input2.size == 1)
      {
        if (site.getClass() != AbraSiteMerge.class)
        {
          return false;
        }

        final AbraSiteMerge merge = (AbraSiteMerge) site;
        if (merge.inputs.size() != 2 || merge.inputs.get(0) != input1 || merge.inputs.get(1) != input2)
        {
          return false;
        }

        continue;
      }

      if (!(site instanceof AbraSiteKnot))
      {
        return false;
      }

      if (input1.size != input2.size)
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) site;
      if (knot.inputs.size() != 2 || knot.inputs.get(0) != input1 || knot.inputs.get(1) != input2)
      {
        return false;
      }

      if (knot.block.specialType != AbraBaseBlock.TYPE_MERGE)
      {
        return false;
      }
    }

    branch.specialType = AbraBaseBlock.TYPE_MERGE;
    return true;
  }

  private boolean evalBranchSpecialNullify(final AbraBlockBranch branch)
  {
    if (branch.inputs.size() <= 1 || branch.sites.size() != branch.outputs.size())
    {
      return false;
    }

    final AbraBaseSite inputFlag = branch.inputs.get(0);
    if (inputFlag.size != 1)
    {
      // first input must be a Bool flag
      return false;
    }

    final AbraBaseSite firstSite = branch.sites.get(0);
    if (!(firstSite instanceof AbraSiteKnot))
    {
      return false;
    }

    final int type = ((AbraSiteKnot) firstSite).block.specialType;
    if (type != AbraBaseBlock.TYPE_NULLIFY_FALSE && type != AbraBaseBlock.TYPE_NULLIFY_TRUE)
    {
      return false;
    }

    for (int i = 0; i < branch.sites.size(); i++)
    {
      final AbraBaseSite site = branch.sites.get(i);
      final AbraBaseSite output = branch.outputs.get(i);
      if (!(site instanceof AbraSiteKnot) || !(output instanceof AbraSiteMerge))
      {
        return false;
      }

      final AbraSiteMerge out = (AbraSiteMerge) output;
      if (out.inputs.size() != 1 || out.inputs.get(0) != site)
      {
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) site;
      if (knot.block.specialType != type)
      {
        // must all be same nullify type
        return false;
      }

      if (knot.inputs.get(0) != inputFlag)
      {
        // everyone should refer the input flag
        return false;
      }

      if (knot.inputs.get(1) != branch.inputs.get(i + 1))
      {
        // should refer a specific input
        return false;
      }
    }

    check(branch.name != null && branch.name.startsWith("nullify"));

    // set nullify type and have a correctly sized null vector ready
    branch.specialType = type;
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

    final AbraBaseSite output = branch.outputs.get(0);
    if (output.getClass() != AbraSiteMerge.class)
    {
      return false;
    }

    final AbraSiteMerge merge = (AbraSiteMerge) output;
    if (merge.inputs.size() != 1 || merge.inputs.get(0) != input)
    {
      return false;
    }

    branch.specialType = AbraBaseBlock.TYPE_SLICE;
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

    if (knot.block instanceof AbraBlockBranch)
    {
      evalKnotBranch(knot);
      return;
    }

    // knot.block is a lut
    ensure(knot.inputs.size() <= 3);

    for (final AbraBaseSite input : knot.inputs)
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

    if (lut.lookup.equals(ConstZeroFuncManager.LUT_ZERO))
    {
      lut.specialType = AbraBaseBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '0');
      return;
    }

    if (lut.lookup.equals(ConstFuncManager.LUT_MIN))
    {
      lut.specialType = AbraBaseBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '-');
      return;
    }

    if (lut.lookup.equals(ConstFuncManager.LUT_ONE))
    {
      lut.specialType = AbraBaseBlock.TYPE_CONSTANT;
      lut.constantValue = new TritVector(1, '1');
      return;
    }

    if (lut.lookup.equals(NullifyFuncManager.LUT_NULLIFY_FALSE))
    {
      lut.specialType = AbraBaseBlock.TYPE_NULLIFY_FALSE;
      return;
    }

    if (lut.lookup.equals(NullifyFuncManager.LUT_NULLIFY_TRUE))
    {
      lut.specialType = AbraBaseBlock.TYPE_NULLIFY_TRUE;
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

    for (final AbraBaseSite input : merge.inputs)
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
    }
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

  private void resolveRecursions(final AbraModule module)
  {
    // did we encounter any missing branch sizes?
    int lastMissing = 0;
    while (missing != lastMissing)
    {
      // try to resolve missing ones by running another pass
      // over the ones that have not been done analyzing yet
      // and see if that results in less missing branch sizes
      lastMissing = missing;
      missing = 0;
      evalBlocks(module.branches);
    }

    if (missing != 0)
    {
      // still missing some branch sizes
      // must be due to recursion issues
      for (final AbraBlockBranch branch : module.branches)
      {
        if (branch.size() == 0)
        {
          Qupla.log("Unresolved trit vector size in branch: " + branch.name);
        }
      }

      error("Recursion issue detected");
    }

    // quick sanity check if everything has a size now
    for (final AbraBlockBranch branch : module.branches)
    {
      for (final AbraBaseSite site : branch.sites)
      {
        if (site.size == 0 && ((AbraSiteMerge) site).inputs.size() != 0)
        {
          error("WTF?");
        }
      }

      for (final AbraBaseSite site : branch.outputs)
      {
        if (site.size == 0 && ((AbraSiteMerge) site).inputs.size() != 0)
        {
          error("WTF?");
        }
      }

      for (final AbraBaseSite site : branch.latches)
      {
        if (site.size == 0 && site.references != 0)
        {
          error("WTF?");
        }
      }
    }
  }
}
