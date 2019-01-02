package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritVector;

public class ConstZeroFuncManager extends BaseFuncManager
{
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

    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + "_" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputValue);
      knot.block = manager.find(context, inputSizes[i]);
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.type = AbraBaseBlock.TYPE_CONSTANT;
    branch.constantValue = new TritVector(size, '0');

    return branch;
  }

  @Override
  protected void generateLut()
  {
    lut = context.abraModule.addLut("constZero_", "000000000000000000000000000");
    lut.type = AbraBaseBlock.TYPE_CONSTANT;
    lut.constantValue = new TritVector(1, '0');
  }

  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + "_" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.block = lut;
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.type = AbraBaseBlock.TYPE_CONSTANT;
    branch.constantValue = new TritVector(size, '0');

    saveBranch(branch);
  }
}
