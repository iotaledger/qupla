package org.iota.qupla.exception;

import org.iota.qupla.parser.Token;

public class CodeException extends RuntimeException
{
  public final Token token;

  public CodeException(final Token token, final String message)
  {
    super(message);
    this.token = token;
  }
}
