package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraBlockBranch extends AbraBlock
{
  public ArrayList<AbraSite> inputs = new ArrayList<>();
  public ArrayList<AbraSite> latches = new ArrayList<>();
  public ArrayList<AbraSite> outputs = new ArrayList<>();
  public int siteNr;
  public ArrayList<AbraSite> sites = new ArrayList<>();
  public int size;

  public AbraSiteParam addInputParam(final int inputSize)
  {
    final AbraSiteParam inputSite = new AbraSiteParam();
    inputSite.size = inputSize;
    inputSite.name = "P" + inputs.size();
    inputs.add(inputSite);
    return inputSite;
  }

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
    for (final AbraSite site : sites)
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

  private void insertNullify(final AbraContext context, final int where, final AbraSite condition, final boolean trueFalse)
  {
    final AbraSite site = sites.get(where);

    // create a site for nullifyFalse<site.size>(conditon, falseBranch)
    final AbraSiteKnot nullify = new AbraSiteKnot();
    nullify.size = site.size;
    nullify.inputs.add(condition);
    condition.references++;
    nullify.inputs.add(site);
    site.references++;
    nullify.nullify(context, trueFalse);

    replaceSite(site, nullify);

    sites.add(where + 1, nullify);
  }

  @Override
  public void markReferences()
  {
    markReferences(inputs);
    markReferences(sites);
    markReferences(outputs);
    markReferences(latches);
  }

  private void markReferences(final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.markReferences();
    }
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
    for (final AbraSite site : sites)
    {
      site.index = siteNr++;
    }
  }

  @Override
  public void optimize(final AbraContext context)
  {
    // first move the nullifies up the chain as far as possible
    for (final AbraSite site : sites)
    {
      site.optimizeNullify();
    }

    // then insert nullify operations and rewire accordingly
    for (int i = 0; i < sites.size(); i++)
    {
      final AbraSite site = sites.get(i);
      if (site.nullifyFalse != null)
      {
        insertNullify(context, i, site.nullifyFalse, false);
        continue;
      }

      if (site.nullifyTrue != null)
      {
        insertNullify(context, i, site.nullifyTrue, true);
      }
    }

    // now bypass superfluous single-input merges
    for (final AbraSite site : sites)
    {
      if (site.getClass() == AbraSiteMerge.class)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) site;
        if (merge.references > 0 && merge.inputs.size() == 1)
        {
          replaceSite(merge, merge.inputs.get(0));
        }
      }
    }

    // and finally remove all unreferenced sites
    for (int i = sites.size() - 1; i >= 0; i--)
    {
      final AbraSite site = sites.get(i);
      if (site.references == 0)
      {
        if (site instanceof AbraSiteMerge)
        {
          final AbraSiteMerge merge = (AbraSiteMerge) site;
          for (final AbraSite input : merge.inputs)
          {
            input.references--;
          }
        }

        if (site.nullifyFalse != null)
        {
          site.nullifyFalse.references--;
        }

        if (site.nullifyTrue != null)
        {
          site.nullifyTrue.references--;
        }

        sites.remove(i);
        if (site.stmt != null)
        {
          if (i < sites.size())
          {
            if (sites.get(i).stmt == null)
            {
              sites.get(i).stmt = site.stmt;
            }
          }
          else
          {
            if (outputs.get(0).stmt == null)
            {
              outputs.get(0).stmt = site.stmt;
            }
          }
        }
      }
    }
  }

  private void putSites(final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.code(tritCode);
    }
  }

  private void replaceSite(final AbraSite site, final AbraSite replacement)
  {
    replaceSite(site, replacement, sites);
    replaceSite(site, replacement, outputs);
    replaceSite(site, replacement, latches);
  }

  private void replaceSite(final AbraSite target, final AbraSite replacement, final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite next : sites)
    {
      if (next instanceof AbraSiteMerge)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) next;
        for (int i = 0; i < merge.inputs.size(); i++)
        {
          if (merge.inputs.get(i) == target)
          {
            target.references--;
            replacement.references++;
            merge.inputs.set(i, replacement);
          }
        }
      }

      if (next.nullifyFalse == target)
      {
        target.references--;
        replacement.references++;
        next.nullifyFalse = replacement;
      }

      if (next.nullifyTrue == target)
      {
        target.references--;
        replacement.references++;
        next.nullifyTrue = replacement;
      }
    }
  }

  @Override
  public String type()
  {
    return "()";
  }
}
