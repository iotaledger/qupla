package org.iota.qupla.dispatcher;

import org.iota.qupla.helper.TritVector;

public class Effect
{
  public int delay;
  public Environment environment;

  public Effect(final Environment environment, final int delay)
  {
    this.environment = environment;
    this.delay = delay;
  }

  public void queueEnvironmentEvents(final TritVector value)
  {
    // transform the effect into one or more entity events in the event queue
    environment.queueEntityEvents(value, delay);
  }
}
