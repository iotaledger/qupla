package org.iota.qupla.helper;

public abstract class Indentable
{
  private boolean mustIndent;
  private int spaces;

  public Indentable append(final String text)
  {
    if (text.length() == 0)
    {
      return this;
    }

    if (mustIndent)
    {
      mustIndent = false;
      for (int i = 0; i < spaces; i++)
      {
        appendify(" ");
      }
    }

    appendify(text);
    return this;
  }

  protected abstract void appendify(String text);

  public Indentable indent()
  {
    spaces += 2;
    return this;
  }

  public Indentable newline()
  {
    append("\n");
    mustIndent = true;
    return this;
  }

  public Indentable undent()
  {
    spaces -= 2;
    return this;
  }
}
