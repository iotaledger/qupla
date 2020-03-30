package org.iota.qupla;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.context.AbraConfigContext;
import org.iota.qupla.abra.context.AbraEvalContext;
import org.iota.qupla.abra.context.AbraPrintContext;
import org.iota.qupla.abra.optimizers.DuplicateSiteOptimizer;
import org.iota.qupla.abra.optimizers.FpgaConfigurationOptimizer;
import org.iota.qupla.abra.optimizers.MultiLutOptimizer;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.dispatcher.entity.FuncEntity;
import org.iota.qupla.dispatcher.entity.ViewEntity;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.exception.ExitException;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.helper.Verilog;
import org.iota.qupla.qupla.context.QuplaEvalContext;
import org.iota.qupla.qupla.context.QuplaPrintContext;
import org.iota.qupla.qupla.context.QuplaToAbraContext;
import org.iota.qupla.qupla.context.QuplaToVerilogContext;
import org.iota.qupla.qupla.context.QuplaToYAMLContext;
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
  public static final ArrayList<String> config = new ArrayList<>();
  private static Dispatcher dispatcher;
  private static final ArrayList<BaseExpr> expressions = new ArrayList<>();
  private static final String[] flags = {
      "-2b",
      "-3b",
      "-abra",
      "-config",
      "-echo",
      "-eval",
      "-fpga",
      "-math",
      "-test",
      "-tree",
      "-view",
      "-yaml",
      };
  private static int openWindows;
  private static final HashSet<String> options = new HashSet<>();
  private static QuplaToAbraContext quplaToAbraContext;
  private static QuplaModule singleModule;

  private static void analyzeExpression(final String statement)
  {
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.lines.add(statement);
    tokenizer.module = singleModule;
    tokenizer.nextToken();
    final BaseExpr expr = new MergeExpr(tokenizer).optimize();
    expr.analyze();
    expressions.add(expr);
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

  private static void evalExpressions()
  {
    for (final BaseExpr expr : expressions)
    {
      log("\nEvaluate: " + expr);
      runEval(expr);
    }
  }

  public static void log(final String text)
  {
    System.out.println(text);
  }

  public static void main(final String[] args)
  {
    setupWindowAdapter();

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
              log("Unrecognized option:" + arg);
            }

            // just ignore the option
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

        startDispatcher();
        processOptions();
        evalExpressions();
        waitOneSecond();
        stopDispatcher();

        // for (final AbraBlockBranch branch : quplaToAbraContext.abraModule.branches)
        // {
        //   final String count = "         " + branch.count;
        //   log(count.substring(count.length() - 9) + "   " + branch.name);
        // }
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
    if (options.contains("-2b"))
    {
      Verilog.bitEncoding(Verilog.BITS_2B);
    }

    if (options.contains("-3b"))
    {
      Verilog.bitEncoding(Verilog.BITS_3B);
    }

    // echo back all modules as source
    if (options.contains("-echo"))
    {
      runEchoSource();
    }

    // emit all modules as Abra tritcode
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

    // emit YAML
    if (options.contains("-yaml"))
    {
      runYAMLGenerator();
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

    // run FuncEntities as Abra instead of Qupla
    FuncEntity.abraModule = quplaToAbraContext.abraModule;

    for (final BaseExpr expr : expressions)
    {
      if (expr instanceof FuncExpr)
      {
        final FuncExpr funcExpr = (FuncExpr) expr;
        if (funcExpr.name != null)
        {
          for (final AbraBlockBranch branch : FuncEntity.abraModule.branches)
          {
            if (branch.name.equals(funcExpr.name))
            {
              branch.fpga = true;
              break;
            }
          }
        }
      }

      break;
    }

    if (options.contains("-config"))
    {
      runAbraToConfig();
    }
  }

  private static void runAbraToConfig()
  {
    log("Generate Configuration");
    final AbraModule module = quplaToAbraContext.abraModule;
    for (final AbraBlockBranch branch : module.branches)
    {
      branch.analyzed = false;
    }

    for (final AbraBlockBranch branch : module.branches)
    {
      if (!branch.analyzed)
      {
        new FpgaConfigurationOptimizer(module, branch).run();
      }
    }

    new AbraPrintContext("FpgaAbra.txt").eval(module);

    for (final AbraBlockBranch branch : module.branches)
    {
      Qupla.log("Optimizing " + branch.name);
      new MultiLutOptimizer(module, branch).run();
      new DuplicateSiteOptimizer(module, branch).run();
    }

    new AbraPrintContext("FpgaAbraOpt.txt").eval(module);

    for (final BaseExpr expr : expressions)
    {
      if (expr instanceof FuncExpr)
      {
        final FuncExpr funcExpr = (FuncExpr) expr;
        if (funcExpr.name != null)
        {
          final AbraConfigContext config = new AbraConfigContext();
          config.funcName = funcExpr.name;
          config.eval(module);
        }
      }
    }
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
      abraEvalContext.eval(quplaToAbraContext, expr);
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
        if (options.contains("-view"))
        {
          // add a viewer for every known environment
          for (final String envName : dispatcher.listEnvironments())
          {
            new ViewEntity(dispatcher, envName);
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
        if (exec.type == Token.TOK_EVAL)
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

    final String value = context.value.toDecimal();
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
        if (exec.type == Token.TOK_TEST)
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

  private static void runYAMLGenerator()
  {
    log("Run YAML generator");
    new QuplaToYAMLContext("Qupla.yml").eval(singleModule);
  }

  private static void setupWindowAdapter()
  {
    ViewEntity.windowAdapter = new WindowAdapter()
    {
      @Override
      public void windowClosed(final WindowEvent windowEvent)
      {
        openWindows--;
        stopDispatcher();
      }

      @Override
      public void windowOpened(final WindowEvent windowEvent)
      {
        openWindows++;
      }
    };
  }

  private static void startDispatcher()
  {
    log("Start dispatcher");
    dispatcher = Dispatcher.getInstance();

    FuncEntity.addEntities(dispatcher, QuplaModule.allModules.values());

    for (final String config : config)
    {
      if (config.startsWith("//#entity"))
      {
        Entity.start(config.split(" ")[1]);
      }
    }
  }

  private static void stopDispatcher()
  {
    if (openWindows == 0)
    {
      log("Stop dispatcher");
      dispatcher.cancel();
    }
  }

  public static String toString(final TritVector value, final TypeStmt typeInfo)
  {
    final String varName = value.name != null ? value.name + ": " : "";
    return varName + "(" + typeInfo.toString(value) + ") " + new String(value.trits());
  }

  private static void waitOneSecond()
  {
    try
    {
      sleep(1000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }
}
