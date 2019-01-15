package org.iota.qupla.dispatcher;

import java.util.ArrayList;

import org.iota.qupla.helper.TritVector;

public class Event
{
  public static int currentQuant;
  private static final ArrayList<Event> queue = new ArrayList<>();

  public Entity entity;
  public int quant;
  public TritVector value;

  public Event(final Entity entity, final TritVector value, final int delay)
  {
    this.entity = entity;
    this.value = value;
    this.quant = currentQuant + delay;
    synchronized (queue)
    {
      queue.add(this);
    }
  }

  public static boolean dispatchCurrentQuantEvents()
  {
    if (queue.size() == 0)
    {
      return false;
    }

    // process every event in the queue that is meant to run in the current quant
    // we may optimize this in the future by ordering the queue by quant number
    int i = 0;
    while (i < queue.size())
    {
      final Event event = queue.get(i);
      if (event.quant <= currentQuant)
      {
        synchronized (queue)
        {
          queue.remove(i);
        }

        event.dispatch();
        continue;
      }

      i++;
    }

    currentQuant++;
    return true;
  }

  public void dispatch()
  {
    entity.runOneWave(value);
  }
}
