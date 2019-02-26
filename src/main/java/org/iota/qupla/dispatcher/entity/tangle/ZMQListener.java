package org.iota.qupla.dispatcher.entity.tangle;

import java.util.HashMap;

import org.zeromq.ZMQ;

public abstract class ZMQListener extends Thread
{
  private final String address;
  private boolean running;
  private ZMQ.Socket socket;
  private HashMap<String, Integer> subscribedTopics = new HashMap<>();
  private int timeoutTolerance;

  public ZMQListener(String address, int timeoutTolerance)
  {
    this.address = address;
    this.timeoutTolerance = timeoutTolerance;
    ZMQ.Context context = ZMQ.context(1);
    socket = context.socket(ZMQ.SUB);
    socket.setReceiveTimeOut(timeoutTolerance);
  }

  private void connect()
  {
    boolean success = false;
    for (int tries = 0; tries < 5 && !success; tries++)
    {
      try
      {
        socket.connect(address);
        success = true;
      }
      catch (Throwable t)
      {
        System.err.println("Retrying to establish connection. Failed because exception thrown: " + t.getMessage());
        t.printStackTrace();
      }
    }

    if (!success)
    {
      throw new RuntimeException("Failed to connect to ZMQ server: '" + address + "'.");
    }
  }

  private void listen()
  {
    running = true;
    while (running)
    {
      byte[] messageBytes = null;
      try
      {
        messageBytes = socket.recv(0);
      }
      catch (final Exception ex)
      {
        // expected
        if (!running)
        {
          break;
        }
      }

      if (messageBytes == null)
      {
        System.err.println("Did not receive message from " + address + " in " + timeoutTolerance + "ms, reconnecting ...");
        connect();
        continue;
      }

      String message = new String(messageBytes);
      processZMQMessage(message);
    }
  }

  public abstract void processZMQMessage(String zmqMessage);

  @Override
  public void run()
  {
    try
    {
      connect();
      listen();
    }
    catch (final Exception ex)
    {
      // expected
      if (running)
      {
        ex.printStackTrace();
      }
    }
  }

  public void subscribe(String topic)
  {
    final Integer count = subscribedTopics.get(topic);
    if (count != null)
    {
      // already listening to this topic
      subscribedTopics.put(topic, count + 1);
      return;
    }

    subscribedTopics.put(topic, 1);
    if (socket != null)
    {
      socket.subscribe(topic);
    }
  }

  public void terminate()
  {
    running = false;
    if (socket != null)
    {
      socket.close();
    }
  }

  public void unsubscribe(String topic)
  {
    final Integer count = subscribedTopics.get(topic);
    if (count != null && count > 1)
    {
      // already listening to this topic
      subscribedTopics.put(topic, count + 1);
      return;
    }

    subscribedTopics.remove(topic);
    if (socket != null)
    {
      socket.unsubscribe(topic);
    }
  }
}
