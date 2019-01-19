package org.iota.qupla.dispatcher;

import java.util.ArrayList;

import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.statement.TypeStmt;

public class Environment
{
  private final ArrayList<Entity> entities = new ArrayList<>();
  public String id;
  public String name;
  public TypeStmt typeInfo;

  public Environment(final String name, final TypeStmt typeInfo)
  {
    this.name = name;
    this.typeInfo = typeInfo;
  }

  public void join(final Entity entity)
  {
    //TODO insert ordered by entity id to be deterministic
    synchronized (entities)
    {
      entities.add(entity);
    }
  }

  public void queueEntityEvents(final TritVector value, final int delay)
  {
    synchronized (entities)
    {
      // create properly delayed events for all entities in this environment
      for (final Entity entity : entities)
      {
        entity.queueEvent(value, delay);
      }
    }
  }

  public void resetEntityLimits()
  {
    synchronized (entities)
    {
      for (final Entity entity : entities)
      {
        entity.resetLimit();
      }
    }
  }
}
