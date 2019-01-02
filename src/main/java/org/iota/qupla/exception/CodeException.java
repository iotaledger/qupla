package org.iota.qupla.exception;

import org.iota.qupla.qupla.parser.Token;

public class CodeException extends RuntimeException
{
  public Token token;

  public CodeException(final String message)
  {
    super(message);
  }

  public CodeException(final Token token, final String message)
  {
    super(message);
    this.token = token;
  }
}
