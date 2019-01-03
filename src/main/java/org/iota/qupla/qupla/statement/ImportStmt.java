package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ImportStmt extends BaseExpr
{
  public Module importModule;

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
    importModule = Module.parse(name);
    size = 1;
  }

  @Override
  public BaseExpr clone()
  {
    return new ImportStmt(this);
  }
}
