package org.iota.qupla.dispatcher;

import java.util.Collection;
import java.util.HashMap;

import org.iota.qupla.qupla.expression.JoinExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.statement.FuncStmt;

public class Dispatcher
{
  private static final HashMap<String, Environment> environments = new HashMap<>();

  public Dispatcher(final Collection<QuplaModule> modules)
  {
    // add all functions in all modules that have join statements
    // as entities to their corresponding environment
    for (final QuplaModule module : modules)
    {
      for (final FuncStmt func : module.funcs)
      {
        for (final BaseExpr envExpr : func.envExprs)
        {
          if (envExpr instanceof JoinExpr)
          {
            final JoinExpr join = (JoinExpr) envExpr;
            final Environment environment = getEnvironment(join.name);
            environment.addEntity(func, join.limit == null ? 1 : join.limit.size);
          }
        }
      }
    }
  }

  public static Environment getEnvironment(final String name)
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

  public void finished()
  {
    synchronized (environments)
    {
      environments.clear();
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
