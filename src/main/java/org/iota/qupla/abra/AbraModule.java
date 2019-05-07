package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
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
  public int version;

  public void addBranch(final AbraBlockBranch branch)
  {
    branches.add(branch);
    blocks.add(branch);
  }

  private void addLut(final AbraBlockLut lut)
  {
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
    numberBlocks(imports);
    numberBlocks(luts);
    numberBlocks(branches);
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
