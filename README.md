# qupla

Java reference implementation of Qupla (QUbic Programming LAnguage) with sample code and initial standard libraries.
* Qupla source code parser
* Qupla interpreter
* Qupla to Abra tritcode translator (mostly done)
* Qupla Debug Info tritcode generator
* Qupla JIT compilation (TBD)
* Qupla Verilog generator (mostly done)
* Abra tritcode interpreter
* Abra tritcode Verilog generator (mostly done)

Set the working folder while running to the resources folder for now.
Or move the resources folder somewhere else so you can add your own Qupla project modules and set the working folder to that location

Command line expects names of project module(s) to be loaded.

Current command line flags

-abra  Emits Qupla/Abra tritcode transformation as commented Qupla source text in resources/Abra.txt

-echo  Echoes back the current code tree as Qupla source

-eval  Runs all eval statements

-fpga  Emits Verilog HDL for further compiling to FPGA as resources/Verilog.txt (requires -trit)

-math  Special test flag to verify results of specific math functions

-test  Runs all test statements (unit tests)

-tree  Generates a textual representation of the Qupla code tree

-view  Start QCM viewers for every environment


Example of Java compilation and running Qupla on Windows command line after extracting sources into \Qupla folder:

    md \Qupla\build
    cd \Qupla\qupla\src\main\java
    "C:\Program Files\Java\jdk1.8.0\bin\javac" -d \Qupla\build org\iota\qupla\Qupla.java
    cd \Qupla\qupla\src\main\resources
    java -classpath \Qupla\build org.iota.qupla.Qupla Examples "fibonacci(10)"



Example of Java compilation and running Qupla on Mac command line after extracting sources into $HOME/Qupla folder:

    cd $HOME/Qupla
    mkdir build
    javac -d $HOME/Qupla/build org/iota/qupla/Qupla.java
    cd $HOME/Qupla/qupla/src/main/resources
    java -classpath $HOME/Qupla/build org.iota.qupla.Qupla Examples "fibonacci(10)"



Qupla statements are grouped in logical source files within a folder whose name specifies the module name these source files belong to.
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
    Duplicate imports will be ignored anyway.
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

    Will execute <expression> and print the result at the console. Activated through -eval flag.
    This supports quick debugging of new Qupla code, especially while running the parser
    in the Java debugger.
    Eval statements will not be included in the final tritcode as they are purely intended
    for debugging purposes.
    See Examples/Factorial for examples of eval statements.
    
test \<value\> = \<expression\>

    Built-in unit tests for functions. Activated through -test flag
    Will execute <expression> and verify that the result equals <value>.
    Will generate a runtime error if <value> does not match the result.
    Test statements will not be included in the final tritcode as they are purely intended
    for debugging purposes.
    See Examples/Factorial for examples of such statements.
    

For more information about the exact syntax of the statements check out the resources/syntax folder
And of course you can study the example code in resources/Qupla and resources/Examples.

More documentation will follow


Wishlist for changes to the language:
* Implement entity aliases for type/lut
* Arithmetic operators (to increase readability)
* Automatic detection of type specifiers on function calls (same)
* Strings?

