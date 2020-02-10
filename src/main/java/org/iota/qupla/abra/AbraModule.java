package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.base.AbraBaseBlock;

public class AbraModule
{
  public static final String SEPARATOR = "_";
  public static final boolean lutAlways3 = false;
  public int blockNr;
  public final ArrayList<AbraBaseBlock> blocks = new ArrayList<>();
  public ArrayList<AbraBlockBranch> branches = new ArrayList<>();
  public final ArrayList<AbraBlockImport> imports = new ArrayList<>();
  public final ArrayList<AbraBlockLut> luts = new ArrayList<>();
  public final ArrayList<AbraBlockSpecial> specials = new ArrayList<>();
  public int version;

  public AbraModule()
  {
    for (final String name : AbraBlockSpecial.names)
    {
      final AbraBlockSpecial block = new AbraBlockSpecial(specials.size(), 1);
      specials.add(block);
      blocks.add(block);
    }

    addLut("constZero_", AbraBlockLut.LUT_ZERO);
    addLut("constOne_", AbraBlockLut.LUT_ONE);
    addLut("constMin_", AbraBlockLut.LUT_MIN);
    addLut("nullifyTrue_", AbraBlockLut.LUT_NULLIFY_TRUE);
    addLut("nullifyFalse_", AbraBlockLut.LUT_NULLIFY_FALSE);
  }

  public void addBranch(final AbraBlockBranch branch)
  {
    branch.index = blocks.size();
    branches.add(branch);
    blocks.add(branch);
  }

  public AbraBlockLut addLut(final String name, final String lookup)
  {
    final AbraBlockLut lut = new AbraBlockLut();
    lut.index = blocks.size();
    lut.name = name;
    lut.lookup = lookup;
    luts.add(lut);
    blocks.add(lut);
    return lut;
  }

  public AbraBaseBlock branch(final String name)
  {
    for (final AbraBlockBranch branch : branches)
    {
      if (branch.name.equals(name))
      {
        return branch;
      }
    }

    return null;
  }

  public AbraBlockLut findLut(final String lutName)
  {
    for (final AbraBlockLut lut : luts)
    {
      if (lut.name.equals(lutName))
      {
        return lut;
      }
    }

    return null;
  }

  public void numberBlocks()
  {
    blockNr = specials.size();
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
    for (final AbraBlockBranch branch : branches)
    {
      branch.markReferences();
    }

    // first optimize lut wrapper functions
    for (int i = 0; i < branches.size(); i++)
    {
      final AbraBlockBranch branch = branches.get(i);
      if (branch.couldBeLutWrapper())
      {
        branch.optimize(this);
      }
    }

    // then optimize all other functions
    for (int i = 0; i < branches.size(); i++)
    {
      final AbraBlockBranch branch = branches.get(i);
      if (!branch.couldBeLutWrapper())
      {
        branch.optimize(this);
      }
    }
  }
}
