package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class NullifyFuncManager extends BaseFuncManager
{
  private static final boolean FALSE_IS_MIN = TritConverter.BOOL_FALSE == '-';
  private static final String LUT_FALSE_IS_MIN = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
  private static final String LUT_FALSE_IS_ZERO = "@-@@0@@1@@-@@0@@1@@-@@0@@1@";
  public static final String LUT_NULLIFY_FALSE = FALSE_IS_MIN ? LUT_FALSE_IS_MIN : LUT_FALSE_IS_ZERO;
  public static final String LUT_NULLIFY_TRUE = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
  private boolean trueFalse;

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

    branch = new AbraBlockBranch();
    branch.name = funcName + AbraModule.SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteParam inputSlice = branch.addInputParam(inputSizes[i]);
      final AbraSiteKnot nullify = new AbraSiteKnot();
      nullify.inputs.add(inputFlag);
      inputFlag.references++;
      nullify.inputs.add(inputSlice);
      inputSlice.references++;
      nullify.block = manager.find(module, inputSizes[i]);
      nullify.size = nullify.block.size();
      branch.sites.add(nullify);
      addOutput(nullify);
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    return branch;
  }

  @Override
  protected void generateLut()
  {
    lut = module.addLut(funcName + AbraModule.SEPARATOR, trueFalse ? LUT_NULLIFY_TRUE : LUT_NULLIFY_FALSE);
    lut.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    lut.constantValue = new TritVector(1, '@');
  }

  @Override
  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    branch = new AbraBlockBranch();
    branch.name = funcName + AbraModule.SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteParam inputSlice = branch.addInputParam(1);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      inputFlag.references++;
      knot.inputs.add(inputSlice);
      inputSlice.references++;
      if (AbraModule.lutAlways3)
      {
        knot.inputs.add(inputSlice);
        inputSlice.references++;
      }

      knot.block = lut;
      knot.size = knot.block.size();
      branch.sites.add(knot);
      addOutput(knot);
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    saveBranch(branch);
  }
}
