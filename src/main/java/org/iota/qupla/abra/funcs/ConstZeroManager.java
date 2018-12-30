package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.helper.TritVector;

public class ConstZeroManager extends AbraFuncManager
{
  public ConstZeroManager()
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
    final ConstZeroManager manager = new ConstZeroManager();
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

    branch.type = AbraBlock.TYPE_CONSTANT;
    final String trail = TritVector.zeroes(size);
    branch.constantValue = new TritVector(trail, size);

    return branch;
  }

  @Override
  protected void generateLut()
  {
    lut = context.abra.addLut("constZero_", "000000000000000000000000000");
    lut.type = AbraBlock.TYPE_CONSTANT;
    lut.constantValue = new TritVector("0", 1);
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

    branch.type = AbraBlock.TYPE_CONSTANT;
    final String trail = TritVector.zeroes(size);
    branch.constantValue = new TritVector(trail, size);

    saveBranch(branch);
  }
}
