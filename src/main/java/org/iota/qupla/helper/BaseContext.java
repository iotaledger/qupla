package org.iota.qupla.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class BaseContext extends Indentable
{
  private File file;
  protected BufferedWriter out;
  private FileWriter writer;

  @Override
  protected void appendify(final String text)
  {
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
      }

      if (writer != null)
      {
        writer.close();
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
      file = new File(fileName);
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
