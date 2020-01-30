package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritVector;

public class ConstZeroFuncManager extends BaseFuncManager
{
  public static final String LUT_ZERO = "000000000000000000000000000";

  public ConstZeroFuncManager()
  {
    super("constZero");
  }

  @Override
  protected void createBaseInstances()
  {
    createStandardBaseInstances();
  }

  @Override
  protected void createInstance()
  {
    createBestFuncFunc();
  }

  @Override
  protected AbraBlockBranch generateFuncFunc(final int inputSize, final Integer[] inputSizes)
  {
    // generate function that use smaller functions
    final ConstZeroFuncManager manager = new ConstZeroFuncManager();
    manager.instances = instances;
    manager.sorted = sorted;

    branch = new AbraBlockBranch();
    branch.name = funcName + AbraModule.SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      AbraBaseSite block = null;
      for (int j = 0; j < branch.sites.size(); j++)
      {
        final AbraBaseSite site = branch.sites.get(j);
        if (site.size == inputSizes[i])
        {
          block = site;
          break;
        }
      }

      if (block == null)
      {
        final AbraSiteKnot knot = new AbraSiteKnot();
        knot.inputs.add(inputValue);
        knot.block = manager.find(module, inputSizes[i]);
        knot.size = knot.block.size();
        branch.sites.add(knot);
        block = knot;
      }

      addOutput(block);
    }

    branch.specialType = AbraBaseBlock.TYPE_CONSTANT;
    branch.constantValue = new TritVector(inputSize, '0');

    return branch;
  }

  @Override
  protected void generateLut()
  {
    lut = module.addLut("constZero" + AbraModule.SEPARATOR, LUT_ZERO);
    lut.specialType = AbraBaseBlock.TYPE_CONSTANT;
    lut.constantValue = new TritVector(1, '0');
  }

  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    branch = new AbraBlockBranch();
    branch.name = funcName + AbraModule.SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    final AbraSiteKnot knot = new AbraSiteKnot();
    knot.inputs.add(inputValue);
    if (AbraModule.lutAlways3)
    {
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
    }

    knot.block = lut;
    knot.size = knot.block.size();
    branch.sites.add(knot);
    for (int i = 0; i < inputSize; i++)
    {
      addOutput(knot);
    }

    branch.specialType = AbraBaseBlock.TYPE_CONSTANT;
    branch.constantValue = new TritVector(inputSize, '0');

    saveBranch(branch);
  }
}
