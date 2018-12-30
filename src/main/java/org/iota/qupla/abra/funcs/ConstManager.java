package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.helper.TritVector;

public class ConstManager extends AbraFuncManager
{
  private static AbraBlockLut constMin;
  private static AbraBlockLut constOne;
  private static AbraBlockLut constZero;
  private static ConstZeroManager zeroManager = new ConstZeroManager();
  public TritVector trits;
  public TritVector value;

  public ConstManager()
  {
    super("const");
  }

  @Override
  protected void createBaseInstances()
  {
    if (constZero != null)
    {
      // already initialized
      return;
    }

    constZero = zeroManager.lut;

    constOne = context.abra.addLut("constOne_", "111111111111111111111111111");
    constOne.type = AbraBlock.TYPE_CONSTANT;
    constOne.constantValue = new TritVector("1", 1);

    constMin = context.abra.addLut("constMin_", "---------------------------");
    constMin.type = AbraBlock.TYPE_CONSTANT;
    constMin.constantValue = new TritVector("i", 1);
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    final AbraSite input = branch.addInputParam(1);

    final AbraSiteKnot siteMin = tritConstant(constMin);
    final AbraSiteKnot siteOne = tritConstant(constOne);
    final AbraSiteKnot siteZero = tritConstant(constZero);

    AbraSiteKnot zeroes = null;
    if (trits.size() < size)
    {
      // need to concatenate rest of zeroes
      zeroes = new AbraSiteKnot();
      zeroes.inputs.add(input);
      zeroes.block = zeroManager.find(context, size);
      zeroes.size = zeroes.block.size();
      branch.sites.add(zeroes);
    }

    final AbraSiteKnot constant = new AbraSiteKnot();
    constant.size = size;
    for (int i = 0; i < trits.size(); i++)
    {
      final char c = trits.trit(i);
      constant.inputs.add(c == '0' ? siteZero : c == '1' ? siteOne : siteMin);
    }

    if (zeroes != null)
    {
      constant.inputs.add(zeroes);
    }

    constant.concat(context);

    branch.type = AbraBlock.TYPE_CONSTANT;
    branch.constantValue = value;

    branch.outputs.add(constant);
  }

  public AbraBlockBranch find(final AbraContext context, final TritVector value)
  {
    this.context = context;
    this.value = value;
    size = value.size();

    // strip off trailing zeroes
    trits = null;
    for (int i = size - 1; i >= 0; i--)
    {
      if (value.trit(i) != '0')
      {
        trits = value.slice(0, i + 1);
        break;
      }
    }

    if (trits == null || trits.size() == 0)
    {
      // all zeroes, pass it on to zero manager
      return zeroManager.find(context, size);
    }

    name = funcName + "_" + size + "_" + trits.trits().replace('-', 'T');
    return findInstance();
  }

  private AbraSiteKnot tritConstant(final AbraBlockLut tritLut)
  {
    final AbraSite input = branch.inputs.get(0);

    final AbraSiteKnot site = new AbraSiteKnot();
    site.name = tritLut.name;
    site.inputs.add(input);
    site.inputs.add(input);
    site.inputs.add(input);
    site.block = tritLut;
    site.size = site.block.size();
    branch.sites.add(site);
    return site;
  }
}
