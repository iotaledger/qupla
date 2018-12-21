package org.iota.qupla.exception;

public class ExitException extends RuntimeException
{
  public ExitException()
  {
    super("Exiting");
  }
}
