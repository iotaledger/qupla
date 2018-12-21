package org.iota.qupla.helper;

public class StateValue
{
  public int hash;
  public byte[] path;
  public int pathLength;
  public TritVector value;

  @Override
  public boolean equals(final Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    final StateValue rhs = (StateValue) o;
    if (pathLength != rhs.pathLength)
    {
      return false;
    }

    for (int i = 0; i < pathLength; i++)
    {
      if (path[i] != rhs.path[i])
      {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    if (hash == 0)
    {
      // cache hash value
      hash = 1;
      for (int i = 0; i < pathLength; i++)
      {
        hash = hash * 31 + path[i];
      }

      // make sure not to calculate again
      if (hash == 0)
      {
        hash = -1;
      }
    }

    return hash;
  }
}
