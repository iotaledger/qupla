# qupla

Java reference implementation of Qupla (QUbic Programming LAnguage) with sample code and initial standard libraries.

The goal of Qupla is twofold:

1. Provide a trinary computer language that implements the Abra specification so that it can function as the programming language for Qubic

2. Lower the barrier for programmers by leveraging existing knowledge as much as possible. This means that we try to keep the language and its behavior as familiar as possible while still providing access to the new and unfamiliar functionality of the Qubic Computation Model.

This repository will provide:
* Qupla source code parser
* Qupla interpreter
* Qupla to Abra tritcode translator
* Qupla Debug Info tritcode generator
* Qupla JIT compilation (to be re-added, needs fixing first)
* Qupla Verilog generator (outdated)
* Qupla to YAML translator (outdated)
* Abra tritcode emulator
* Abra tritcode Verilog generator (outdated)

Set the working folder while running to the resources folder for now.
Or move the resources folder somewhere else so you can add your own Qupla project
modules and set the working folder to that location

The command line contains a mix of command line arguments that can be options,
module names, and expressions. Options begin with a dash. Undefined options will
be ignored. Any argument that contains an open parenthesis '(' will be processed
as an expression. Anything else is interpreted as a module name and should match
a folder name in the working folder that contains the Qupla files for that module.

Current command line options

- -2b  
  Use 2b trit encoding when emitting Verilog code (default)

- -3b  
  Use 3b trit encoding when emitting Verilog code

- -abra  
  Generate Abra tritcode from the compiled Qupla source code. The Abra tritcode will
  also be written as an Abra psuedo-code source text file in Abra.txt

- -config  
  Generates a configuration file that can be loaded into an FPGA for the first
  function that is passed as an expression. Make sure to heed any warning about
  the requested function being too large to fit on the FPGA!
  
- -echo  
  Echoes back the current code tree as a single Qupla source code text file in Qupla.txt

- -eval  
  Runs all eval statements

- -fpga  
  Emits Verilog HDL for further synthesis to FPGA in Verilog.txt (requires -trit)

- -math  
  Special test flag to exercise specific math functions and verify their results

- -test  
  Runs all module test statements (unit tests)

- -tree  
  Generates a textual representation of the Qupla code tree in QuplaTree.txt

- -view  
  Start QCM viewers for every environment in the Supervisor to be able to track effects

- -yaml  
  Generate YAML from the compiled Qupla source code to Qupla.yml


Example of Java compilation and running Qupla on Windows command line after extracting sources into \Qupla folder:

    md \Qupla\build
    cd \Qupla\qupla\src\main\java
    "C:\Program Files\Java\jdk1.8.0\bin\javac" -d \Qupla\build org\iota\qupla\Qupla.java
    cd \Qupla\qupla\src\main\resources
    java -classpath \Qupla\build org.iota.qupla.Qupla Fibonacci "fibonacci2<Int>(10)"



Example of Java compilation and running Qupla on Mac command line after extracting sources into $HOME/Qupla folder:

    cd $HOME/Qupla
    mkdir build
    javac -d $HOME/Qupla/build org/iota/qupla/Qupla.java
    cd $HOME/Qupla/qupla/src/main/resources
    java -classpath $HOME/Qupla/build org.iota.qupla.Qupla Fibonacci "fibonacci2<Int>(10)"



Qupla statements are grouped in logical source files within a folder whose name specifies the module that these source files belong to.
Within a module folder all Qupla source files are first parsed and loaded recursively into one big code tree.
The parse phase will only do lexical parsing and syntax checks.
All statements in the module source files are gathered in no particular order and grouped together by statement type.
Once an entire module is loaded the analysis phase will be run`that does semantic analysis on the code tree.

Quick overview of Qupla statements, in order of semantic analysis:

import \<module\>

    Imports all entities from the external module named <module> into the current module
    <module> is the name of the folder that contains the imported module source files
    Note that only a single import statement is necessary within a module due to
    the gathering of all sources within a module into a single code tree.
    Duplicate imports will be ignored.
    When imported modules define identical entity names any definition within the current module
    takes precedence. Explicit entity name resolution can be achieved by explicityly providing
    the desired module name.

type \<typename\> ...

    Declares a type name for a trit vector of specific size and/or structure.
    Within a module all type names must be unique.
    
lut \<lutname\> ...

    Declares a LUT (Look-Up Table).
    Within a module all lut names must be unique.
    
func \<typename\> \<funcname\> ...

    Declares a function.
    Within a module all function names must be unique.
       
template \<templatename\> ...

    Declares a template. A template is a group of types and functions that can be instantiated
    as a unit for different specific trit vector sizes.
    Within a module template names must be unique.
    
use \<templatename\> ...

    Declares specific template instantiation(s). Will instantiate the given template for given
    specific trit vector sizes.
    
eval \<expression\>

    Will execute <expression> and print the result at the console. Activated through -eval option.
    This supports quick debugging of new Qupla code, especially while running the parser
    in the Java debugger.
    Eval statements will not be included in the final tritcode as they are purely intended
    for debugging purposes.
    See Examples/Factorial for examples of eval statements.
    
test \<value\> = \<expression\>

    Built-in unit tests for functions. Activated through -test option.
    Will execute <expression> and verify that the result equals <value>.
    Will generate a runtime error if <value> does not match the result.
    Test statements will not be included in the final tritcode as they are purely intended
    for unit testing purposes.
    See Examples/Factorial for examples of such statements.
    

For more information about the exact syntax of the statements check out the resources/syntax folder
And of course you can study the example code in resources/Qupla and resources/Examples and others.

More documentation will follow


Wishlist for changes to the language:
* Implement entity aliases for type/lut
* Arithmetic operators (to increase readability)
* Automatic detection of type specifiers on function calls (same)
* Strings?

