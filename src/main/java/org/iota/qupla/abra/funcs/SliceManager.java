package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;

public class SliceManager extends AbraFuncManager
{
  public int length;
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

    final AbraSiteParam inputSite = branch.addInputParam(length);

    if (size - start - length > 0)
    {
      branch.addInputParam(size - start - length);
    }

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = size;
    merge.inputs.add(inputSite);
    branch.outputs.add(merge);
  }

  public AbraBlockBranch find(final AbraContext context, final int size, final int start, final int length)
  {
    this.context = context;
    this.size = size;
    this.start = start;
    this.length = length;
    name = "$" + funcName + "$" + size + "$" + start + "$" + length;
    return findInstance();
  }
}
