package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;

public class MergeFuncManager extends BaseFuncManager
{
  public MergeFuncManager()
  {
    super("merge");
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    if (size == 1)
    {
      final AbraSiteParam input1 = branch.addInputParam(1);
      final AbraSiteParam input2 = branch.addInputParam(1);
      generateMergeFunc(input1, input2);
      return;
    }

    if ((size & 1) == 0)
    {
      final int halfSize = size / 2;
      final AbraSiteParam input1_1 = branch.addInputParam(halfSize);
      final AbraSiteParam input1_2 = branch.addInputParam(halfSize);
      final AbraSiteParam input2_1 = branch.addInputParam(halfSize);
      final AbraSiteParam input2_2 = branch.addInputParam(halfSize);
      generateMergeFunc(input1_1, input2_1);
      generateMergeFunc(input1_2, input2_2);
      return;
    }

    final int halfSize = (size - 1) / 2;
    final AbraSiteParam input1_1 = branch.addInputParam(1);
    final AbraSiteParam input1_2 = branch.addInputParam(halfSize);
    final AbraSiteParam input1_3 = branch.addInputParam(halfSize);
    final AbraSiteParam input2_1 = branch.addInputParam(1);
    final AbraSiteParam input2_2 = branch.addInputParam(halfSize);
    final AbraSiteParam input2_3 = branch.addInputParam(halfSize);
    generateMergeFunc(input1_1, input2_1);
    generateMergeFunc(input1_2, input2_2);
    generateMergeFunc(input1_3, input2_3);
  }

  private void generateMerge(final AbraBaseSite input1, final AbraBaseSite input2)
  {
    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = 1;
    merge.inputs.add(input1);
    input1.references++;
    merge.inputs.add(input2);
    input2.references++;
    branch.specialType = AbraBaseBlock.TYPE_MERGE;
    branch.sites.add(merge);
    addOutput(merge);
  }

  private void generateMergeFunc(final AbraBaseSite input1, final AbraBaseSite input2)
  {
    if (input1.size == 1)
    {
      generateMerge(input1, input2);
      return;
    }

    final MergeFuncManager manager = new MergeFuncManager();
    manager.instances = instances;
    manager.sorted = sorted;

    final AbraSiteKnot mergeFunc = new AbraSiteKnot();
    mergeFunc.size = input1.size;
    mergeFunc.block = manager.find(module, input1.size);
    mergeFunc.inputs.add(input1);
    input1.references++;
    mergeFunc.inputs.add(input2);
    input2.references++;
    branch.specialType = AbraBaseBlock.TYPE_MERGE;
    branch.sites.add(mergeFunc);
    addOutput(mergeFunc);
  }
}
