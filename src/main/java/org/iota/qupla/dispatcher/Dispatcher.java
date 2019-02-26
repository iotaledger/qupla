package org.iota.qupla.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;

import org.iota.qupla.qupla.statement.TypeStmt;

public class Dispatcher extends Thread
{
  private static Dispatcher dispatcher;
  private final HashMap<String, Environment> environments = new HashMap<>();
  private boolean stopRunning;

  public static Dispatcher getInstance()
  {
    if (dispatcher == null)
    {
      dispatcher = new Dispatcher();
      dispatcher.start();
    }

    return dispatcher;
  }

  public void cancel()
  {
    stopRunning = true;
  }

  private void finished()
  {
    synchronized (environments)
    {
      environments.clear();
    }

    for (final Entity entity : Entity.entities)
    {
      entity.stop();
    }

    Entity.entities.clear();
  }

  public Environment getEnvironment(final String name, final TypeStmt typeInfo)
  {
    // find or create the named environment
    synchronized (environments)
    {
      final Environment env = environments.get(name);
      if (env != null)
      {
        if (env.typeInfo == null)
        {
          env.typeInfo = typeInfo;
        }
        return env;
      }

      final Environment newEnv = new Environment(name, typeInfo);
      environments.put(newEnv.name, newEnv);
      return newEnv;
    }
  }

  public ArrayList<String> listEnvironments()
  {
    synchronized (environments)
    {
      return new ArrayList<>(environments.keySet());
    }
  }

  @Override
  public void run()
  {
    try
    {
      while (!stopRunning)
      {
        runQuants();
        sleep(200);
      }

      dispatcher = null;
      finished();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  private void runQuants()
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
