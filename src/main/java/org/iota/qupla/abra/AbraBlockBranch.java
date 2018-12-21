package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;

public class AbraBlockBranch extends AbraBlock
{
  public ArrayList<AbraSite> inputs = new ArrayList<>();
  public ArrayList<AbraSite> latches = new ArrayList<>();
  public ArrayList<AbraSite> outputs = new ArrayList<>();
  public int siteNr;
  public ArrayList<AbraSite> sites = new ArrayList<>();
  public int size;

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).newline().indent();

    numberSites();

    appendSites(context, inputs, "input");
    appendSites(context, sites, "body");
    appendSites(context, outputs, "output");
    appendSites(context, latches, "latch");

    return context.undent();
  }

  private void appendSites(final CodeContext context, final ArrayList<? extends AbraSite> sites, final String type)
  {
    for (AbraSite site : sites)
    {
      site.type = type;
      site.append(context).newline();
    }
  }

  @Override
  public void code()
  {
    numberSites();

    putSites(inputs);
    putSites(sites);
    putSites(outputs);
    putSites(latches);
  }

  private void numberSites()
  {
    siteNr = 0;
    numberSites(inputs);
    numberSites(sites);
    numberSites(outputs);
    numberSites(latches);
  }

  private void numberSites(final ArrayList<? extends AbraSite> sites)
  {
    for (AbraSite site : sites)
    {
      site.index = siteNr++;
    }
  }

  private void putSites(final ArrayList<? extends AbraSite> sites)
  {
    for (AbraSite site : sites)
    {
      site.code(tritCode);
    }
  }

  @Override
  public String type()
  {
    return "()";
  }
}
