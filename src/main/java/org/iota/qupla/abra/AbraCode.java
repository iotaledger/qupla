package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.context.AbraCodeContext;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraCode
{
  public TritCode abra = new TritCode();
  public int blockNr;
  public ArrayList<AbraBlock> blocks = new ArrayList<>();
  public ArrayList<AbraBlockBranch> branches = new ArrayList<>();
  public ArrayList<AbraBlockImport> imports = new ArrayList<>();
  public ArrayList<AbraBlockLut> luts = new ArrayList<>();

  public void addBranch(final AbraBlockBranch branch)
  {
    branches.add(branch);
    blocks.add(branch);
  }

  public void addLut(final AbraBlockLut lut)
  {
    luts.add(lut);
    blocks.add(lut);
  }

  public AbraBlockLut addLut(final String name, final String trits)
  {
    final AbraBlockLut lut = new AbraBlockLut();
    lut.name = name;
    lut.tritCode.putTrits(trits);
    addLut(lut);
    return lut;
  }

  public void append(final CodeContext context)
  {
    // numberBlocks();
    // temporarily use a different numbering order
    // this makes it easier to compare optimization pass outputs
    // because the block numbers aren't constantly changed
    blockNr = 0;
    numberBlocks(blocks);

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

  public void eval(final AbraCodeContext context)
  {
    context.abraCode = this;

    numberBlocks();

    context.started();

    for (final AbraBlockImport imp : imports)
    {
      imp.eval(context);
    }

    for (final AbraBlockLut lut : luts)
    {
      lut.eval(context);
    }

    for (final AbraBlockBranch branch : branches)
    {
      branch.eval(context);
    }

    context.finished();
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
