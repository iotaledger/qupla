package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;

public class SliceManager extends AbraFuncManager
{
  public int start;

  public SliceManager()
  {
    super("slice");
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    if (start > 0)
    {
      branch.addInputParam(start);
    }

    final AbraSiteParam inputSite = branch.addInputParam(size);

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = size;
    merge.inputs.add(inputSite);

    branch.type = AbraBlock.TYPE_SLICE;
    branch.offset = start;
    branch.outputs.add(merge);
  }

  public AbraBlockBranch find(final AbraContext context, final int size, final int start)
  {
    this.context = context;
    this.size = size;
    this.start = start;
    name = funcName + "_" + size + "_" + start;
    return findInstance();
  }
}
