package org.iota.qupla.dispatcher;

import java.util.HashMap;

public class Dispatcher
{
  private final HashMap<String, Environment> environments = new HashMap<>();

  public void finished()
  {
    synchronized (environments)
    {
      environments.clear();
    }
  }

  public Environment getEnvironment(final String name)
  {
    // find or create the named environment
    synchronized (environments)
    {
      final Environment env = environments.get(name);
      if (env != null)
      {
        return env;
      }

      final Environment newEnv = new Environment(name);
      environments.put(newEnv.name, newEnv);
      return newEnv;
    }
  }

  public void runQuants()
  {
    // keep running as long as there are still events in the queue
    while (Event.dispatchCurrentQuantEvents())
    {
      // reset all invocation limits for the next quant
      synchronized (environments)
      {
        for (final Environment environment : environments.values())
        {
          environment.resetEntityLimits();
        }
      }
    }
  }
}
