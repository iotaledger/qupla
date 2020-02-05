package org.iota.qupla.abra.context;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.exception.CodeException;

public class AbraConfigContext extends AbraBaseContext
{
  public AbraBlockBranch funcBranch;
  public String funcName = "add_9";
  private BufferedOutputStream outputStream;

  public void eval(final AbraModule module)
  {
    module.blockNr = 0;
    module.blocks.clear();

    for (final AbraBlockBranch branch : module.branches)
    {
      if (branch.name.equals(funcName))
      {
        funcBranch = branch;
        break;
      }
    }

    if (funcBranch == null)
    {
      throw new CodeException("Cannot fing branch: " + funcName);
    }

    // find all LUTs this branch uses
    for (final AbraBaseSite site : funcBranch.sites)
    {
      if (site instanceof AbraSiteKnot)
      {
        final AbraSiteKnot knot = (AbraSiteKnot) site;
        if (!module.blocks.contains(knot.block))
        {
          module.blocks.add(knot.block);
        }
      }
    }

    module.numberBlocks(module.blocks);

    try
    {
      outputStream = new BufferedOutputStream(new FileOutputStream(funcName + ".qbc"));

      write(module.blocks.size());
      evalBlocks(module.blocks);
      write(1);
      evalBranch(funcBranch);

      outputStream.close();
    }
    catch (final Exception ex)
    {
      throw new CodeException("Cannot write to " + funcName + ".qbc");
    }
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    branch.numberSites();
    write(branch.inputs.size());

    write(branch.sites.size());
    for (final AbraBaseSite site : branch.sites)
    {
      site.eval(this);
    }

    write(branch.outputs.size());
    for (final AbraBaseSite output : branch.outputs)
    {
      write(output.index);
    }
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    throw new CodeException("Import for config?");
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    write(knot.block.index);
    write(knot.inputs.size());
    for (final AbraBaseSite input : knot.inputs)
    {
      write(input.index);
    }
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    throw new CodeException("Latch for config?");
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    for (int group = 0; group < 32; group += 8)
    {
      int bytes = 0;
      for (int i = 7; i >= 0; i--)
      {
        bytes <<= 2;
        int index = group + i;
        if (index < lut.lookup.length())
        {
          switch (lut.lookup.charAt(index))
          {
          case '0':
            bytes |= 0x03;
            break;
          case '1':
            bytes |= 0x01;
            break;
          case '-':
            bytes |= 0x02;
            break;
          }
        }
      }
      write(bytes);
    }
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    throw new CodeException("Param for config?");
  }

  private void write(final int value)
  {
    try
    {
      outputStream.write(new byte[] {
          (byte) value,
          (byte) (value >> 8)
      });
    }
    catch (Exception ex)
    {
      throw new CodeException("Config write failed");
    }
  }
}
