package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.context.AbraContext;

public class ConstManager extends AbraFuncManager
{
  private static AbraBlockLut constMin;
  private static AbraBlockLut constOne;
  private static AbraBlockLut constZero;
  private static ConstZeroManager zeroManager = new ConstZeroManager();
  public String trits;

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
    constMin = context.abra.addLut("constMin_", "---------------------------");
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
    if (trits.length() < size)
    {
      // need to concatenate rest of zeroes
      zeroes = new AbraSiteKnot();
      zeroes.size = size;
      zeroes.inputs.add(input);
      zeroes.block = zeroManager.find(context, size);
      branch.sites.add(zeroes);
    }

    final AbraSiteKnot constant = new AbraSiteKnot();
    constant.size = size;
    for (int i = 0; i < trits.length(); i++)
    {
      final char c = trits.charAt(i);
      constant.inputs.add(c == '0' ? siteZero : c == '1' ? siteOne : siteMin);
    }

    if (zeroes != null)
    {
      constant.inputs.add(zeroes);
    }

    constant.concat(context);
    branch.outputs.add(constant);
  }

  public AbraBlockBranch find(final AbraContext context, final String value)
  {
    this.context = context;
    size = value.length();

    // strip off trailing zeroes
    trits = "";
    for (int i = size - 1; i >= 0; i--)
    {
      if (value.charAt(i) != '0')
      {
        trits = value.substring(0, i + 1);
        break;
      }
    }

    if (trits.length() == 0)
    {
      // all zeroes, pass it on to zero manager
      return zeroManager.find(context, size);
    }

    name = funcName + "_" + size + "_" + trits.replace('-', 'T');
    return findInstance();
  }

  private AbraSiteKnot tritConstant(final AbraBlockLut tritLut)
  {
    final AbraSite input = branch.inputs.get(0);

    final AbraSiteKnot site = new AbraSiteKnot();
    site.size = 3;
    site.name = tritLut.name;
    site.inputs.add(input);
    site.inputs.add(input);
    site.inputs.add(input);
    site.block = tritLut;
    branch.sites.add(site);
    return site;
  }
}
