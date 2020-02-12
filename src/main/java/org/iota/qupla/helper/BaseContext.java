package org.iota.qupla.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class BaseContext extends Indentable
{
  protected BufferedWriter out;
  public String string;
  private FileWriter writer;

  @Override
  protected void appendify(final String text)
  {
    if (string != null)
    {
      string += text;
      return;
    }

    if (out != null)
    {
      fileWrite(text);
    }
  }

  protected void fileClose()
  {
    try
    {
      if (out != null)
      {
        out.close();
        out = null;
      }

      if (writer != null)
      {
        writer.close();
        writer = null;
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  protected void fileOpen(final String fileName)
  {
    try
    {
      final File file = new File(fileName);
      writer = new FileWriter(file);
      out = new BufferedWriter(writer);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

  private void fileWrite(final String text)
  {
    try
    {
      out.write(text);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }
}
