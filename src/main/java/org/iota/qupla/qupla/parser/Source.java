package org.iota.qupla.qupla.parser;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.ImportStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.TemplateStmt;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

public class Source extends BaseExpr
{
  public String pathName;

  public Source(final Tokenizer tokenizer, final String pathName)
  {
    super(tokenizer);

    this.pathName = pathName;
    module.currentSource = this;

    origin = tokenizer.nextToken();

    while (tokenizer.tokenId() != Token.TOK_EOF)
    {
      switch (tokenizer.tokenId())
      {
      case Token.TOK_EVAL:
        module.execs.add(new ExecStmt(tokenizer, false));
        break;

      case Token.TOK_FUNC:
        module.funcs.add(new FuncStmt(tokenizer));
        break;

      case Token.TOK_IMPORT:
        module.imports.add(new ImportStmt(tokenizer));
        break;

      case Token.TOK_LUT:
        module.luts.add(new LutStmt(tokenizer));
        break;

      case Token.TOK_TEMPLATE:
        module.templates.add(new TemplateStmt(tokenizer));
        break;

      case Token.TOK_TEST:
        module.execs.add(new ExecStmt(tokenizer, true));
        break;

      case Token.TOK_TYPE:
        module.types.add(new TypeStmt(tokenizer));
        break;

      case Token.TOK_USE:
        module.uses.add(new UseStmt(tokenizer));
        break;

      default:
        final Token token = tokenizer.currentToken();
        error(token, "Unexpected token: '" + token.text + "'");
      }
    }
  }

  @Override
  public void analyze()
  {
  }

  @Override
  public BaseExpr clone()
  {
    return null;
  }

  @Override
  public String toString()
  {
    return pathName;
  }
}
