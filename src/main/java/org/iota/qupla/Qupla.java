package org.iota.qupla;

import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.abra.context.AbraEvalContext;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.dispatcher.FuncEntity;
import org.iota.qupla.dispatcher.GameOfLifeEntity;
import org.iota.qupla.dispatcher.ViewEntity;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.exception.ExitException;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaEvalContext;
import org.iota.qupla.qupla.context.QuplaPrintContext;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.context.QuplaToVerilogContext;
import org.iota.qupla.qupla.context.QuplaTreeViewerContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.VectorExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

import static java.lang.Thread.sleep;

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
      "-view",
      };
  private static final HashSet<String> options = new HashSet<>();
  private static QuplaToAbraContext quplaToAbraContext;
  private static QuplaModule singleModule;

  private static BaseExpr analyzeExpression(final String statement)
  {
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.lines.add(statement);
    tokenizer.module = singleModule;
    tokenizer.nextToken();
    final BaseExpr expr = new MergeExpr(tokenizer).optimize();
    expr.analyze();
    return expr;
  }

  private static void codeException(final CodeException ex)
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
      final ArrayList<BaseExpr> types = use.typeArgs;

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
    runEval(analyzeExpression(statement));
  }

  public static void log(final String text)
  {
    System.out.println(text);
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

          QuplaModule.parse(arg);
        }

        singleModule = new QuplaModule(QuplaModule.allModules.values());

        for (final String arg : args)
        {
          // interpret as expression to be evaluated
          // can be a function call, but also an expression surrounded by ( and )
          // here we make sure that all necessary template instantiations are pulled in
          if (arg.contains("("))
          {
            analyzeExpression(arg);
          }
        }

        singleModule.analyze();

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

    // display the code tree
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
    quplaToAbraContext = new QuplaToAbraContext();
    quplaToAbraContext.eval(singleModule);
  }

  private static void runEchoSource()
  {
    log("Generate Qupla.txt");
    new QuplaPrintContext().eval(singleModule);
  }

  private static void runEval(final BaseExpr expr)
  {
    log("Eval: " + expr.toString());

    final QuplaEvalContext context = new QuplaEvalContext();

    long mSec = System.currentTimeMillis();
    if (options.contains("-abra"))
    {
      final AbraEvalContext abraEvalContext = new AbraEvalContext();
      abraEvalContext.eval(quplaToAbraContext.abraModule, expr);
      context.value = abraEvalContext.value;
    }
    else
    {
      expr.eval(context);
    }

    mSec = System.currentTimeMillis() - mSec;

    log("  ==> " + toString(context.value, expr.typeInfo));
    log("Time: " + mSec + " ms");

    if (expr instanceof FuncExpr)
    {
      final FuncExpr funcExpr = (FuncExpr) expr;
      if (funcExpr.func.envExprs.size() != 0)
      {
        // there may be affect statements in there
        final Dispatcher dispatcher = Dispatcher.getInstance();
        FuncEntity.addEntities(dispatcher, QuplaModule.allModules.values());

        if (options.contains("-view"))
        {
          // add a viewer for every known environment
          for (final String envName : dispatcher.listEnvironments())
          {
            new ViewEntity(dispatcher, envName);
            if (envName.equals("gameOfLife"))
            {
              new GameOfLifeEntity();
              new GameOfLifeEntity();
              new GameOfLifeEntity();
            }
          }
        }

        // note that this is a dummy entity, only used to send effects
        // it will only affect its associated environments but does not
        // join any environments to receive resulting effects
        // any resulting effects will be picked up by the instance that
        // was previously created by FuncEntity.addEntities()
        final Entity entity = new FuncEntity(funcExpr.func, 0, dispatcher);
        entity.queueEffectEvents(context.value);
      }
    }
  }

  private static void runEvals()
  {
    long mSec = System.currentTimeMillis();
    for (final QuplaModule module : QuplaModule.allModules.values())
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
    new QuplaToVerilogContext().eval(singleModule);
  }

  private static void runMathTest(final QuplaEvalContext context, final FuncExpr expr, final int lhs, final int rhs, final int result)
  {
    final VectorExpr lhsArg = (VectorExpr) expr.args.get(0);
    lhsArg.vector = new TritVector(TritConverter.fromLong(lhs)).slicePadded(0, lhsArg.size);
    final VectorExpr rhsArg = (VectorExpr) expr.args.get(1);
    rhsArg.vector = new TritVector(TritConverter.fromLong(rhs)).slicePadded(0, rhsArg.size);

    expr.eval(context);

    final String value = TritConverter.toDecimal(context.value.trits()).toString();
    if (!value.equals(Integer.toString(result)))
    {
      lhsArg.name = Integer.toString(lhs);
      rhsArg.name = Integer.toString(rhs);
      log(expr + " = " + result + ", found: " + value + ", inputs: " + lhs + " and " + rhs);
    }
  }

  private static void runMathTests()
  {
    //final String statement = "fullAdd<Tiny>(1,1,0)";
    //final String statement = "fullMul<Tiny>(1,1)";
    final String statement = "div<Tiny>(1,1)";
    log("\nEvaluate: " + statement);
    long mSec = System.currentTimeMillis();
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.lines.add(statement);
    tokenizer.module = singleModule;
    tokenizer.nextToken();
    final FuncExpr expr = (FuncExpr) new MergeExpr(tokenizer).optimize();
    expr.analyze();
    final QuplaEvalContext context = new QuplaEvalContext();
    for (int lhs = 2100; lhs <= 9841; lhs += 1)
    {
      log("Iteration: " + lhs);
      for (int rhs = lhs; rhs <= 9841; rhs += 1)
      {
        //runMathTest(context, expr, lhs, rhs, lhs + rhs);
        //runMathTest(context, expr, lhs, rhs, lhs * rhs);
        runMathTest(context, expr, lhs, rhs, lhs / rhs);
      }
    }

    mSec = System.currentTimeMillis() - mSec;
    log("Time: " + mSec + " ms");
  }

  private static void runTest(final ExecStmt exec)
  {
    log("Test: " + exec.expected + " = " + exec.expr);

    final QuplaEvalContext context = new QuplaEvalContext();

    long mSec = System.currentTimeMillis();
    if (options.contains("-abra"))
    {
      final AbraEvalContext abraEvalContext = new AbraEvalContext();
      abraEvalContext.eval(quplaToAbraContext.abraModule, exec.expr);
      context.value = abraEvalContext.value;
    }
    else
    {
      exec.expr.eval(context);
    }

    mSec = System.currentTimeMillis() - mSec;

    if (!exec.succeed(context.value))
    {
      final String lhs = exec.expr.typeInfo.toString(exec.expected.vector);
      final String rhs = exec.expr.typeInfo.toString(context.value);
      exec.error("Test expected " + lhs + " but found " + rhs);
    }

    log("Time: " + mSec + " ms");
  }

  private static void runTests()
  {
    long mSec = System.currentTimeMillis();
    for (final QuplaModule module : QuplaModule.allModules.values())
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
    log("Run Tree Viewer");
    new QuplaTreeViewerContext().eval(singleModule);
  }

  public static String toString(final TritVector value, final TypeStmt typeInfo)
  {
    final String varName = value.name != null ? value.name + ": " : "";
    return varName + "(" + typeInfo.toString(value) + ") " + value.trits();
  }
}
