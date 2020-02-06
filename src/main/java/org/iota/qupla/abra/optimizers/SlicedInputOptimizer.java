package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

public class SlicedInputOptimizer extends BaseOptimizer implements Comparator<AbraSiteKnot>
{
  private final ArrayList<AbraSiteKnot> concats = new ArrayList<>();
  private final ArrayList<AbraSiteParam> inputs = new ArrayList<>();
  private final ArrayList<AbraSiteKnot> knots = new ArrayList<>();
  private final TreeSet<AbraSiteKnot> slicers = new TreeSet<>(this);

  public SlicedInputOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private boolean canSlice(final AbraBaseSite input)
  {
    knots.clear();
    slicers.clear();

    for (final AbraSiteKnot knot : branch.sites)
    {
      switch (knot.block.specialType)
      {
      case AbraBaseBlock.TYPE_CONSTANT:
        continue;

      case AbraBaseBlock.TYPE_SLICE:
        if (knot.inputs.size() == 1 && knot.inputs.get(0) == input)
        {
          knots.add(knot);
          slicers.add(knot);
          continue;
        }
        break;
      }

      for (final AbraBaseSite in : knot.inputs)
      {
        if (in == input)
        {
          return false;
        }
      }
    }

    if (slicers.size() == 0)
    {
      // nothing to do
      return false;
    }

    // check that no slices overlap
    int prevOffset = 0;
    for (final AbraSiteKnot slicer : slicers)
    {
      final AbraBlockBranch branch = (AbraBlockBranch) slicer.block;
      if (branch.offset < prevOffset)
      {
        // not contiguous
        return false;
      }

      prevOffset = branch.offset + branch.size;
    }

    return true;
  }

  @Override
  public int compare(final AbraSiteKnot lhs, final AbraSiteKnot rhs)
  {
    final AbraBlockBranch lhsBranch = (AbraBlockBranch) lhs.block;
    final AbraBlockBranch rhsBranch = (AbraBlockBranch) rhs.block;
    if (lhsBranch.offset != rhsBranch.offset)
    {
      return lhsBranch.offset < rhsBranch.offset ? -1 : 1;
    }

    if (lhsBranch.size != rhsBranch.size)
    {
      return lhsBranch.size < rhsBranch.size ? -1 : 1;
    }

    return 0;
  }

  private void doSlice(final AbraSiteParam input)
  {
    final ArrayList<AbraSiteParam> slices = new ArrayList<>();
    int prevOffset = 0;
    for (final AbraSiteKnot slicer : slicers)
    {
      final AbraBlockBranch branch = (AbraBlockBranch) slicer.block;
      if (branch.offset != prevOffset)
      {
        // unused part of input, insert dummy sliced input
        final AbraSiteParam slice = makeSlice(input, prevOffset, branch.offset - prevOffset);
        slices.add(slice);
      }

      // create corresponding sliced input
      final AbraSiteParam slice = makeSlice(input, branch.offset, branch.size);
      slices.add(slice);
      prevOffset = branch.offset + branch.size;
    }

    if (input.size != prevOffset)
    {
      // unused final part of input, insert dummy sliced input
      final AbraSiteParam slice = makeSlice(input, prevOffset, input.size - prevOffset);
      slices.add(slice);
    }

    for (final AbraSiteKnot knot : knots)
    {
      final AbraBlockBranch branch = (AbraBlockBranch) knot.block;
      for (final AbraSiteParam slice : slices)
      {
        if (slice.offset == branch.offset)
        {
          replaceSite(knot, slice);
          break;
        }
      }
    }

    final AbraSiteKnot concat = new AbraSiteKnot();
    concat.size = input.size;
    concat.slice(0);
    for (final AbraSiteParam slice : slices)
    {
      concat.inputs.add(slice);
      slice.references++;
    }

    replaceSite(input, concat);
    concats.add(concat);
    inputs.addAll(slices);
  }

  private AbraSiteParam makeSlice(final AbraBaseSite input, final int sliceOffset, final int sliceSize)
  {
    final AbraSiteParam slice = new AbraSiteParam();
    slice.offset = sliceOffset;
    slice.size = sliceSize;
    if (input.name != null)
    {
      slice.name = input.name + "_" + sliceSize + "_" + sliceOffset;
    }

    return slice;
  }

  @Override
  public void run()
  {
    for (final AbraSiteParam input : branch.inputs)
    {
      if (!canSlice(input))
      {
        // keep this one
        inputs.add(input);
        continue;
      }

      doSlice(input);
    }

    // did we slice anything?
    if (concats.size() != 0)
    {
      // this makes sure that the offsets are updated correctly
      branch.inputs.clear();
      for (final AbraSiteParam input : inputs)
      {
        branch.addInput(input);
      }

      // any constant knots need to be updated with the new first param
      final AbraBaseSite constInput = inputs.get(0);
      for (AbraSiteKnot knot : branch.sites)
      {
        if (knot.block.specialType == AbraBaseBlock.TYPE_CONSTANT)
        {
          knot.inputs.get(0).references--;
          knot.inputs.set(0, constInput);
          constInput.references++;
        }
      }

      branch.sites.addAll(0, concats);
    }
  }
}
