package org.iota.qupla.dispatcher;

import java.util.ArrayList;

import org.iota.qupla.context.EvalContext;
import org.iota.qupla.expression.AffectExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.statement.FuncStmt;

public class Entity
{
  public static final EvalContext evalContext = new EvalContext();

  public final ArrayList<Effect> effects = new ArrayList<>();
  public FuncStmt func;
  public String id;
  public int invoked;
  public int limit;

  public Entity(final FuncStmt func, final int limit)
  {
    this.func = func;
    this.limit = limit;

    // determine the list of effects that this entity produces
    for (final BaseExpr envExpr : func.envExprs)
    {
      if (envExpr instanceof AffectExpr)
      {
        final AffectExpr affect = (AffectExpr) envExpr;
        final Environment env = Dispatcher.getEnvironment(affect.name);
        addEffect(env, affect.delay == null ? 0 : affect.delay.size);
      }
    }
  }

  public void addEffect(final Environment env, final int delay)
  {
    final Effect effect = new Effect(env, delay);

    //TODO insert ordered by env id to be deterministic
    effects.add(effect);
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

  public void runWave(final TritVector inputValue)
  {
    // have the entity process the value
    final TritVector returnValue = evalContext.evalEntity(this, inputValue);

    // queue any effects that the entity triggered
    queueEffectEvents(returnValue);
  }
}
