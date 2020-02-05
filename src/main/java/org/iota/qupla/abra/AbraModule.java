package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;

public class AbraModule
{
  public static final String SEPARATOR = "_";
  public static final int SPECIAL_LUTS = 5;
  public static final boolean lutAlways3 = false;
  public int blockNr;
  public final ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  public ArrayList<AbraBlockBranch> branches = new ArrayList<>();
  public final ArrayList<AbraBlockImport> imports = new ArrayList<>();
  public final ArrayList<AbraBlockLut> luts = new ArrayList<>();
  public int version;

  public AbraModule()
  {
    final AbraBlockLut merge = addLut("merge", AbraBlockLut.NULL_LUT);
    merge.specialType = AbraBaseBlock.TYPE_MERGE;

    final AbraBlockLut nullifyTrue = addLut("nullifyTrue", AbraBlockLut.LUT_NULLIFY_TRUE);
    nullifyTrue.specialType = AbraBaseBlock.TYPE_NULLIFY_TRUE;

    final AbraBlockLut nullifyFalse = addLut("nullifyFalse", AbraBlockLut.LUT_NULLIFY_FALSE);
    nullifyFalse.specialType = AbraBaseBlock.TYPE_NULLIFY_FALSE;

    final AbraBlockLut constant = addLut("constant", AbraBlockLut.NULL_LUT);
    constant.specialType = AbraBaseBlock.TYPE_CONSTANT;

    final AbraBlockLut slice = addLut("slice", AbraBlockLut.NULL_LUT);
    slice.specialType = AbraBaseBlock.TYPE_SLICE;
  }

  public void addBranch(final AbraBlockBranch branch)
  {
    branch.index = luts.size() + branches.size();
    branches.add(branch);
    blocks.add(branch);
  }

  private void addLut(final AbraBlockLut lut)
  {
    lut.index = luts.size();
    luts.add(lut);
    blocks.add(lut);
  }

  public AbraBlockLut addLut(final String name, final String lookup)
  {
    final AbraBlockLut lut = new AbraBlockLut();
    lut.name = name;
    lut.lookup = lookup;
    addLut(lut);
    return lut;
  }

  public AbraBaseBlock branch(final String name)
  {
    for (final AbraBaseBlock branch : branches)
    {
      if (branch.name.equals(name))
      {
        return branch;
      }
    }

    return null;
  }

  public void numberBlocks()
  {
    blockNr = 0;
    numberBlocks(luts);
    numberBlocks(branches);
    numberBlocks(imports);
  }

  public void numberBlocks(final ArrayList<? extends AbraBaseBlock> blocks)
  {
    for (final AbraBaseBlock block : blocks)
    {
      block.index = blockNr++;
    }
  }

  public void optimize()
  {
    // determine reference counts for branches and sites
    for (final AbraBaseBlock branch : branches)
    {
      branch.markReferences();
    }

    // first optimize lut wrapper functions
    for (int i = 0; i < branches.size(); i++)
    {
      final AbraBaseBlock branch = branches.get(i);
      if (branch.couldBeLutWrapper())
      {
        branch.optimize(this);
      }
    }

    // then optimize all other functions
    for (int i = 0; i < branches.size(); i++)
    {
      final AbraBaseBlock branch = branches.get(i);
      if (!branch.couldBeLutWrapper())
      {
        branch.optimize(this);
      }
    }
  }
}
