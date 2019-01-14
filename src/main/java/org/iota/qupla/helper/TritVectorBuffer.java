package org.iota.qupla.helper;

public class TritVectorBuffer
{
  private static final int INITIAL_SIZE = 27;

  public char[] buffer;
  public int used;

  public TritVectorBuffer(final int size)
  {
    int newSize = INITIAL_SIZE;
    while (newSize < size)
    {
      newSize *= 3;
    }

    buffer = new char[newSize];
    used = size;
  }

  public void grow(final int needed)
  {
    if (buffer.length < needed)
    {
      int newSize = buffer.length * 3;
      while (newSize < needed)
      {
        newSize *= 3;
      }

      final char[] newBuffer = new char[newSize];
      System.arraycopy(buffer, 0, newBuffer, 0, used);

      buffer = newBuffer;
    }
  }

  @Override
  public String toString()
  {
    return new String(buffer, 0, used);
  }
}
