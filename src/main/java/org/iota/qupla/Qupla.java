package org.iota.qupla;

import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.abra.context.AbraEvalContext;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.exception.ExitException;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaEvalContext;
import org.iota.qupla.qupla.context.QuplaPrintContext;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.context.QuplaToVerilogContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.IntegerExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.UseStmt;

public class Qupla
{
  private static final String[] flags = {
      "-abra",
      "-echo",
      "-eval",
      "-fpga",
      "-math",
      "-test",
      "-tree",
      };
  private static final HashSet<String> options = new HashSet<>();
  private static QuplaToAbraContext quplaToAbraContext;

  public static void codeException(final CodeException ex)
  {
    final Token token = ex.token;
    if (token == null)
    {
      log("  ... Error:  " + ex.getMessage());
      ex.printStackTrace(System.out);
      throw new ExitException();
    }

    //TODO actual filename?
    if (token.source == null)
    {
      log("  ... Error:  " + ex.getMessage());
      ex.printStackTrace(System.out);
      throw new ExitException();
    }

    final String path = token.source.pathName;
    final String fileName = path.substring(path.lastIndexOf('/') + 1);
    log("  ...(" + fileName + ":" + (token.lineNr + 1) + ") Error:  " + ex.getMessage());
    if (BaseExpr.currentUse != null)
    {
      final UseStmt use = BaseExpr.currentUse;
      final ArrayList<BaseExpr> types = use.typeInstantiations.get(BaseExpr.currentUseIndex);

      String name = use.name;
      boolean first = true;
      for (final BaseExpr type : types)
      {
        name += first ? "<" : ", ";
        first = false;
        name += type;
      }

      log("  Function instantiated by: use " + name + ">");
    }

    ex.printStackTrace(System.out);
    throw new ExitException();
  }

  private static void evalExpression(final String statement)
  {
    log("\nEvaluate: " + statement);
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.lines.add(statement);
    tokenizer.module = new Module(new ArrayList<>());
    tokenizer.module.modules.addAll(Module.allModules.values());
    tokenizer.nextToken();
    final BaseExpr expr = new MergeExpr(tokenizer).optimize();
    expr.analyze();
    runEval(expr);
  }

  public static void log(final String text)
  {
    BaseExpr.logLine(text);
  }

  public static void main(final String[] args)
  {
    try
    {
      try
      {
        for (final String arg : args)
        {
          for (final String flag : flags)
          {
            if (flag.equals(arg))
            {
              options.add(flag);
              break;
            }
          }

          if (arg.startsWith("-"))
          {
            if (!options.contains(arg))
            {
              log("Unrecognized argument:" + arg);
            }

            continue;
          }

          // expression to be evaluated later
          if (arg.contains("("))
          {
            continue;
          }

          Module.parse(arg);
        }

        processOptions();

        for (final String arg : args)
        {
          // interpret as expression to be evaluated
          // can be a function call, but also an expression surrounded by ( and )
          if (arg.contains("("))
          {
            evalExpression(arg);
          }
        }
      }
      catch (final CodeException ex)
      {
        codeException(ex);
      }
    }
    catch (final ExitException ex)
    {
    }
  }

  private static void processOptions()
  {
    // echo back all modules as source
    if (options.contains("-echo"))
    {
      runEchoSource();
    }

    // echo back all modules as Abra tritcode
    if (options.contains("-abra"))
    {
      runAbraGenerator();
    }

    // emit verilog code to Verilog.txt
    if (options.contains("-fpga"))
    {
      runFpgaGenerator();
    }

    // display the syntax tree
    if (options.contains("-tree"))
    {
      runTreeViewer();
    }

    // run all unit test comments
    if (options.contains("-math"))
    {
      runMathTests();
    }

    // run all unit test comments
    if (options.contains("-test"))
    {
      runTests();
    }

    // run all evaluation comments
    if (options.contains("-eval"))
    {
      runEvals();
    }
  }

  private static void runAbraGenerator()
  {
    log("Run Abra generator");
    final Module singleModule = new Module(Module.allModules.values());
    quplaToAbraContext = new QuplaToAbraContext();
    quplaToAbraContext.eval(singleModule);
  }

  private static void runEchoSource()
  {
    log("Generate Qupla.txt");
    final Module singleModule = new Module(Module.allModules.values());
    new QuplaPrintContext().eval(singleModule);
  }

  private static void runEval(final BaseExpr expr)
  {
    log("Eval: " + expr.toString());

    final QuplaEvalContext context = new QuplaEvalContext();
    final AbraEvalContext abraEvalContext = new AbraEvalContext();

    long mSec = System.currentTimeMillis();
    if (options.contains("-abra"))
    {
      abraEvalContext.eval(quplaToAbraContext, expr);
      context.value = abraEvalContext.value;
    }
    else
    {
      expr.eval(context);
    }

    mSec = System.currentTimeMillis() - mSec;

    log("  ==> " + expr.typeInfo.display(context.value));
    log("Time: " + mSec + " ms");

    if (expr instanceof FuncExpr)
    {
      final Dispatcher dispatcher = new Dispatcher(Module.allModules.values());
      final FuncExpr funcExpr = (FuncExpr) expr;
      context.createEntityEffects(funcExpr.func);
      dispatcher.runQuants();
      dispatcher.finished();
    }
  }

  private static void runEvals()
  {
    long mSec = System.currentTimeMillis();
    for (final Module module : Module.allModules.values())
    {
      for (final ExecStmt exec : module.execs)
      {
        if (exec.expected == null)
        {
          runEval(exec.expr);
        }
      }
    }

    mSec = System.currentTimeMillis() - mSec;
    log("All evals: " + mSec + " ms\n");
  }

  private static void runFpgaGenerator()
  {
    log("Run Verilog compiler");
    final Module singleModule = new Module(Module.allModules.values());
    new QuplaToVerilogContext().eval(singleModule);
  }

  private static void runMathTest(final QuplaEvalContext context, final FuncExpr expr, final int lhs, final int rhs, final int result)
  {
    final IntegerExpr lhsArg = (IntegerExpr) expr.args.get(0);
    lhsArg.vector = new TritVector(TritConverter.fromLong(lhs)).slicePadded(0, lhsArg.size);
    final IntegerExpr rhsArg = (IntegerExpr) expr.args.get(1);
    rhsArg.vector = new TritVector(TritConverter.fromLong(rhs)).slicePadded(0, rhsArg.size);

    expr.eval(context);

    final String value = context.value.displayValue(0, 0);
    if (!value.equals(Integer.toString(result)))
    {
      lhsArg.name = Integer.toString(lhs);
      rhsArg.name = Integer.toString(rhs);
      log(expr + " = " + result + ", found: " + context.value + ", inputs: " + lhsArg.vector + " and " + rhsArg.vector);
    }
  }

  private static void runMathTests()
  {
    final String statement = "fullAdd<Tiny>(1,1,0)";
    log("\nEvaluate: " + statement);
    long mSec = System.currentTimeMillis();
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.lines.add(statement);
    tokenizer.module = new Module(new ArrayList<>());
    tokenizer.module.modules.addAll(Module.allModules.values());
    tokenizer.nextToken();
    final FuncExpr expr = (FuncExpr) new MergeExpr(tokenizer).optimize();
    expr.analyze();
    final QuplaEvalContext context = new QuplaEvalContext();
    for (int lhs = 0; lhs <= 9841; lhs += 1)
    {
      for (int rhs = 0; rhs <= 9841; rhs += 1)
      {
        runMathTest(context, expr, lhs, rhs, lhs + rhs);
      }
    }

    mSec = System.currentTimeMillis() - mSec;
    log("Time: " + mSec + " ms");
  }

  private static void runTest(final ExecStmt exec)
  {
    log("Test: " + exec.expected + " = " + exec.expr);

    final QuplaEvalContext context = new QuplaEvalContext();
    final AbraEvalContext abraEvalContext = new AbraEvalContext();

    long mSec = System.currentTimeMillis();
    if (options.contains("-abra"))
    {
      abraEvalContext.eval(quplaToAbraContext, exec.expr);
      context.value = abraEvalContext.value;
    }
    else
    {
      exec.expr.eval(context);
    }

    mSec = System.currentTimeMillis() - mSec;

    if (!exec.succeed(context.value))
    {
      final String lhs = exec.expr.typeInfo.displayValue(exec.expected.vector);
      final String rhs = exec.expr.typeInfo.displayValue(context.value);
      exec.error("Test expected " + lhs + " but found " + rhs);
    }

    log("Time: " + mSec + " ms");
  }

  private static void runTests()
  {
    long mSec = System.currentTimeMillis();
    for (final Module module : Module.allModules.values())
    {
      for (final ExecStmt exec : module.execs)
      {
        if (exec.expected != null)
        {
          runTest(exec);
        }
      }
    }

    mSec = System.currentTimeMillis() - mSec;
    log("All tests: " + mSec + " ms\n");
  }

  private static void runTreeViewer()
  {
    //TODO
  }
}
