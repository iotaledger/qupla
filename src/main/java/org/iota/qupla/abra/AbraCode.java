package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraCode
{
  public TritCode abra = new TritCode();
  public int blockNr;
  public ArrayList<AbraBlockBranch> branches = new ArrayList<>();
  public ArrayList<AbraBlockImport> imports = new ArrayList<>();
  public ArrayList<AbraBlockLut> luts = new ArrayList<>();

  public void append(final CodeContext context)
  {
    numberBlocks();

    appendBlocks(context, imports);
    appendBlocks(context, luts);
    appendBlocks(context, branches);
  }

  private void appendBlocks(final CodeContext context, final ArrayList<? extends AbraBlock> blocks)
  {
    for (AbraBlock block : blocks)
    {
      block.append(context);
    }
  }

  public void code()
  {
    numberBlocks();

    abra.putInt(0); // version
    putBlocks(imports);
    putBlocks(luts);
    putBlocks(branches);
  }

  private void numberBlocks()
  {
    blockNr = 0;
    numberBlocks(imports);
    numberBlocks(luts);
    numberBlocks(branches);
  }

  private void numberBlocks(final ArrayList<? extends AbraBlock> blocks)
  {
    for (final AbraBlock block : blocks)
    {
      block.index = blockNr++;
    }
  }

  public void optimize(final AbraContext context)
  {
    // determine reference counts for branches and sites
    for (final AbraBlock branch : branches)
    {
      branch.markReferences();
    }

    for (int i = 0; i < branches.size(); i++)
    {
      final AbraBlock branch = branches.get(i);
      branch.optimize(context);
    }
  }

  private void putBlocks(final ArrayList<? extends AbraBlock> blocks)
  {
    abra.putInt(blocks.size());
    for (final AbraBlock block : blocks)
    {
      block.code(abra);
    }
  }
}
