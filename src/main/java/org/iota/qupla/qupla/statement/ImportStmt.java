package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ImportStmt extends BaseExpr
{
  public QuplaModule importModule;

  public ImportStmt(final ImportStmt copy)
  {
    super(copy);
    importModule = copy.importModule;
  }

  public ImportStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_IMPORT, "import");

    final Token firstPart = expect(tokenizer, Token.TOK_NAME, "module name");
    name = firstPart.text;
  }

  @Override
  public void analyze()
  {
    importModule = QuplaModule.parse(name);
    size = 1;
  }

  @Override
  public BaseExpr clone()
  {
    return new ImportStmt(this);
  }
}
