package org.iota.qupla.dispatcher;

import java.util.ArrayList;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.TritVector;

public abstract class Entity
{
  public static final ArrayList<Entity> entities = new ArrayList<>();

  public final ArrayList<Effect> effects = new ArrayList<>();
  public String id;
  public int invoked;
  public int limit;

  public Entity(final int limit)
  {
    this.limit = limit;
    entities.add(this);
  }

  public static void start(final String entityName)
  {
    try
    {
      final Class<?> entityClass = Class.forName(entityName);
      entityClass.newInstance();
    }
    catch (final Exception e)
    {
      throw new CodeException("Cannot instantiate entity class name: " + entityName);
    }
  }

  public void affect(final Environment env, final int delay)
  {
    final Effect effect = new Effect(env, delay);

    //TODO insert ordered by env id to be deterministic
    effects.add(effect);
  }

  public void join(final Environment env)
  {
    env.join(this);
  }

  public abstract TritVector onEffect(final TritVector effect);

  public void processEffect(final TritVector effect)
  {
    // have the entity process the value
    final TritVector returnValue = onEffect(effect);
    if (returnValue == null || returnValue.isNull())
    {
      // propagation stops if null is returned (no data flow)
      return;
    }

    // queue any effects that the entity triggered
    queueEffectEvents(returnValue);
  }

  public void queueEffectEvents(final TritVector value)
  {
    // all effects for this entity have been predetermined already
    // from the metadata, so we just need to queue them as events
    for (final Effect effect : effects)
    {
      effect.queueEnvironmentEvents(value);
    }
  }

  public void queueEvent(final TritVector value, final int delay)
  {
    // queue an event for this entity with proper delay
    final Event event = new Event(this, value, delay);
    if (delay == 0 && invoked < limit)
    {
      // can do another invocation during the current quant
      invoked++;
    }
    else
    {
      // invocation limit exceeded, schedule for next quant
      event.quant++;
    }
  }

  public void resetLimit()
  {
    invoked = 0;
  }

  public void stop()
  {
  }
}
