package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritVector;

public class NullifyFuncManager extends BaseFuncManager
{
  public boolean trueFalse;

  public NullifyFuncManager(final boolean trueFalse)
  {
    super(trueFalse ? "nullifyTrue" : "nullifyFalse");
    this.trueFalse = trueFalse;
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
    final NullifyFuncManager manager = new NullifyFuncManager(trueFalse);
    manager.instances = instances;
    manager.sorted = sorted;

    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(inputSizes[i]);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.block = manager.find(context, inputSizes[i]);
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    return branch;
  }

  @Override
  protected void generateLut()
  {
    final String trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
    final String falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
    lut = context.abraModule.addLut(funcName + SEPARATOR, trueFalse ? trueTrits : falseTrits);
    lut.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    lut.constantValue = new TritVector(1, '@');
  }

  @Override
  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(1);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.block = lut;
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    saveBranch(branch);
  }
}
