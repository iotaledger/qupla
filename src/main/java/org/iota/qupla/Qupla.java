package org.iota.qupla;

// running from command line after extracting sources into \Qupla folder:
// md \Qupla\build
// cd \Qupla\qupla\src\main\java
// "C:\Program Files\Java\jdk1.8.0\bin\javac" -d \Qupla\build org\iota\qupla\Qupla.java
// cd \Qupla\qupla\src\main\resources
// java -classpath \Qupla\build org.iota.qupla.Qupla Examples "fibonacci(10)"

import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.EvalContext;
import org.iota.qupla.context.FpgaContext;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.exception.ExitException;
import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.MergeExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.parser.Module;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;
import org.iota.qupla.statement.ExecStmt;
import org.iota.qupla.statement.UseStmt;

public class Qupla
{
  private static final String[] flags = {
      "-abra",
      "-echo",
      "-eval",
      "-fpga",
      "-test",
      "-tree",
      };
  private static final HashSet<String> options = new HashSet<>();

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

          // interpret as expression to be evaluated
          // can be a function call, but also an expression surrounded by ( and )
          if (arg.contains("("))
          {
            evalExpression(arg);
            continue;
          }

          Module.parse(arg);
        }

        processOptions();
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

    // run all unit test comments
    if (options.contains("-test"))
    {
      runTestComments();
    }

    // run all evaluation comments
    if (options.contains("-eval"))
    {
      runEvalComments();
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
  }

  private static void runAbraGenerator()
  {
    log("Run Abra generator");
    final Module singleModule = new Module(Module.allModules.values());
    singleModule.eval(new AbraContext());
  }

  private static void runEchoSource()
  {

  }

  private static void runEval(final BaseExpr expr)
  {
    log("Eval: " + expr.toString());

    long mSec = System.currentTimeMillis();
    final EvalContext context = new EvalContext();
    expr.eval(context);
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

  private static void runEvalComments()
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
    log("All evals: " + mSec + " ms");
  }

  private static void runFpgaGenerator()
  {
    log("Run Verilog compiler");
    final Module singleModule = new Module(Module.allModules.values());
    final FpgaContext verilogCompiler = new FpgaContext();
    singleModule.eval(verilogCompiler);
    verilogCompiler.cleanup();
  }

  private static void runTest(final ExecStmt exec)
  {
    log("Test: " + exec.toString());

    long mSec = System.currentTimeMillis();
    final EvalContext context = new EvalContext();
    exec.expr.eval(context);
    mSec = System.currentTimeMillis() - mSec;

    if (!exec.succeed(context.value))
    {
      final String lhs = exec.expr.typeInfo.displayValue(exec.expected.vector);
      final String rhs = exec.expr.typeInfo.displayValue(context.value);
      exec.error("Test expected " + lhs + " but found " + rhs);
    }

    log("Time: " + mSec + " ms");
  }

  private static void runTestComments()
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
    log("All tests: " + mSec + " ms");
  }

  private static void runTreeViewer()
  {

  }
}
