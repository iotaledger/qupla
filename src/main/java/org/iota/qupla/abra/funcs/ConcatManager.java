package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;

public class ConcatManager extends AbraFuncManager
{
  public ConcatManager()
  {
    super("concat");
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    final AbraSiteParam inputSite = branch.addInputParam(size);

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = size;
    merge.inputs.add(inputSite);
    branch.outputs.add(merge);
  }
}
