package org.iota.qupla.abra;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class FpgaClient
{
  private String host = "192.168.7.1"; // "192.168.1.76";
  private InputStream inStream;
  private OutputStream outStream;
  private int port = 6666;
  private Socket socket = null;

  public boolean connect()
  {
    try
    {
      socket = new Socket(host, port);
      socket.setTcpNoDelay(true);
      inStream = socket.getInputStream();
      outStream = new BufferedOutputStream(socket.getOutputStream());
      return true;
    }
    catch (UnknownHostException e)
    {
      System.err.println("Cannot find host called: " + host);
    }
    catch (IOException e)
    {
      System.err.println("Could not establish connection for " + host);
    }

    disconnect();
    return false;
  }

  public void disconnect()
  {
    try
    {
      if (inStream != null)
      {
        inStream.close();
      }

      if (outStream != null)
      {
        outStream.close();
      }

      if (socket != null)
      {
        socket.close();
      }
    }
    catch (IOException e)
    {
      System.err.println("Error in cleanup");
    }

    inStream = null;
    outStream = null;
    socket = null;
  }

  public byte[] process(final char command, final byte[] data)
  {
    if (socket == null && !connect())
    {
      return null;
    }

    try
    {
      final byte[] cmdLen = new byte[3];
      cmdLen[0] = (byte) command;
      cmdLen[1] = (byte) (data.length & 0xff);
      cmdLen[2] = (byte) ((data.length >> 8) & 0xff);
      outStream.write(cmdLen);
      if (data.length > 0)
      {
        outStream.write(data);
      }
      outStream.flush();

      int length = inStream.read(cmdLen);
      if (length != 3 || cmdLen[0] != 1)
      {
        System.err.println("READ FAILED");
        disconnect();
        return null;
      }

      length = ((cmdLen[2] & 0xff) << 8) | (cmdLen[1] & 0xff);
      final byte[] output = new byte[length];
      if (length > 0)
      {
        length = inStream.read(output);
        if (length != output.length)
        {
          System.err.println("READ FAILED");
          disconnect();
          return null;
        }
      }

      return output;
    }
    catch (Exception e)
    {
      System.err.println("process failed with host " + host);
      e.printStackTrace();
      disconnect();
      return null;
    }
  }
}
