package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class SlicedInputOptimizer extends BaseOptimizer implements Comparator<AbraBlockBranch>
{
  private final ArrayList<AbraSiteKnot> constants = new ArrayList<>();
  private final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
  private final ArrayList<AbraSiteKnot> knots = new ArrayList<>();
  private final TreeSet<AbraBlockBranch> slicers = new TreeSet<>(this);

  public SlicedInputOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private boolean canSlice(final AbraBaseSite input, final ArrayList<AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      if (!(site instanceof AbraSiteMerge))
      {
        continue;
      }

      final AbraSiteMerge merge = (AbraSiteMerge) site;
      if (!merge.inputs.contains(input))
      {
        // input not used in this site
        continue;
      }

      if (!(merge instanceof AbraSiteKnot))
      {
        // used in merge, cannot pre-slice
        return false;
      }

      final AbraSiteKnot knot = (AbraSiteKnot) merge;
      if (knot.block.specialType != AbraBaseBlock.TYPE_SLICE)
      {
        if (knot.block.specialType == AbraBaseBlock.TYPE_CONSTANT)
        {
          // special case, will need to update input for constant
          constants.add(knot);
          continue;
        }

        // used in knot that is not slicer, cannot pre-slice
        return false;
      }

      if (knot.inputs.size() != 1)
      {
        // not a slicer but a concatenator
        return false;
      }

      knots.add(knot);
      slicers.add((AbraBlockBranch) knot.block);
    }

    return true;
  }

  @Override
  public int compare(final AbraBlockBranch lhs, final AbraBlockBranch rhs)
  {
    if (lhs.offset != rhs.offset)
    {
      return lhs.offset < rhs.offset ? -1 : 1;
    }

    if (lhs.size != rhs.size)
    {
      return lhs.size < rhs.size ? -1 : 1;
    }

    if (lhs.index != rhs.index)
    {
      return lhs.index < rhs.index ? -1 : 1;
    }

    return 0;
  }

  private AbraSiteParam makeSlice(final AbraBaseSite input, final int sliceOffset, final int sliceSize)
  {
    final AbraSiteParam slice = new AbraSiteParam();
    slice.size = sliceSize;
    if (input.name != null)
    {
      slice.name = input.name + "_" + sliceSize + "_" + sliceOffset;
    }

    if (input.varName != null)
    {
      slice.varName = input.varName + "_" + sliceSize + "_" + sliceOffset;
    }
    return slice;
  }

  private boolean preSlice(final AbraBaseSite input)
  {
    if (slicers.size() == 0)
    {
      // nothing to do
      return false;
    }

    // check that no slices overlap
    int prevOffset = 0;
    for (final AbraBlockBranch slicer : slicers)
    {
      if (slicer.offset < prevOffset)
      {
        // overlappping slices
        return false;
      }

      prevOffset = slicer.offset + slicer.size;
    }

    final ArrayList<AbraBlockBranch> slicers = new ArrayList<>(this.slicers);
    final ArrayList<AbraSiteParam> slices = new ArrayList<>();
    prevOffset = 0;
    for (final AbraBlockBranch slicer : slicers)
    {
      if (slicer.offset != prevOffset)
      {
        // unused part of input, insert dummy sliced input
        final AbraSiteParam slice = makeSlice(input, prevOffset, slicer.offset - prevOffset);
        inputs.add(slice);
      }

      // create corresponding sliced input
      final AbraSiteParam slice = makeSlice(input, slicer.offset, slicer.size);
      slices.add(slice);
      inputs.add(slice);
      prevOffset = slicer.offset + slicer.size;
    }

    if (input.size != prevOffset)
    {
      // unused final part of input, insert dummy sliced input
      final AbraSiteParam slice = makeSlice(input, prevOffset, input.size - prevOffset);
      inputs.add(slice);
    }

    for (final AbraSiteKnot knot : knots)
    {
      final int slicerIndex = slicers.indexOf((AbraBlockBranch) knot.block);
      replaceSite(knot, slices.get(slicerIndex));
    }

    return true;
  }

  @Override
  public void run()
  {
    for (index = 0; index < branch.inputs.size(); index++)
    {
      final AbraBaseSite input = branch.inputs.get(index);
      knots.clear();
      slicers.clear();
      if (canSlice(input, branch.sites) && //
          canSlice(input, branch.outputs) && //
          canSlice(input, branch.latches) && //
          preSlice(input))
      {
        continue;
      }

      inputs.add(input);
    }

    // did we pre-slice anything?
    if (inputs.size() != branch.inputs.size())
    {
      branch.inputs = inputs;

      // any constant knots need to replace the old first param
      // with the new first param as their input
      final AbraBaseSite constInput = inputs.get(0);
      for (final AbraSiteKnot constant : constants)
      {
        constant.inputs.clear();
        constant.inputs.add(constInput);
        constInput.references++;
      }
    }
  }
}
