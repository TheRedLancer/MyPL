import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public class REPL {
    // read in user input
    // pull the input through the lexer -> parser -> staticChecker -> codeGenerator
    // We need a living static checker
    // living type-info for storing functions and generated types
    // Need a Global VM Frame?
    TypeInfo typeInfo;
    SymbolTable symbolTable;
    Scanner in;
    InputStream tokenIn;
    VM vm;
    VMFrame globalFrame;
    REPLCodeGenerator generator;
    String lastCommand;
    FileInputStream io;
    // Read in line
    // if line contains an opening bracket (multi-line statement)
    //      continue reading until all open brackets are closed
    // give to lexer
    // if first token is not 'fun' or 'type' we need to run it globally
    // any expressions will be added to special "itAssignStmt" with id "IT" then check the varname in the CodeGenerator and print it
    // maybe call parser, static checker and code gen manually for each line input?
    // create statements object here, then pass to parser to fill in statement??
    // Change Program to also have a list of statements
    
    public void run(Scanner s) {
        System.out.println("Welcome to the MyPL REPL, type ':help' for help");
        String userIn;
        boolean runLastCommand = false;
        String lastCommand = "";
        typeInfo = new TypeInfo();
        symbolTable = new SymbolTable();
        symbolTable.pushEnvironment();
        vm = new VM();
        globalFrame = new VMFrame("global", 0);
        generator = new REPLCodeGenerator(typeInfo, vm, globalFrame);
        boolean loadFile = false;
        boolean done = false;

        while (!done) {
            if (runLastCommand) {
                userIn = lastCommand;
                runLastCommand = false;
            } else if (loadFile) {
                userIn = ":h";
                try {
                    userIn = new String(io.readAllBytes());
                    //System.out.println("Reading from file...: " + userIn);
                    io.close();
                } catch (IOException e) {
                    System.out.println("There was an error in reading the file");
                }
                io = null;
            } else {
                System.out.print("MyPL>>");
                userIn = s.nextLine();
            }
            if (!loadFile && !runLastCommand) {
                while (charsInString(userIn, '{') != charsInString(userIn, '}')) {
                    System.out.print("....>>");
                    userIn += "\n" + s.nextLine();
                }
            }
            loadFile = false;
            if (userIn.length() > 0 && userIn.charAt(0) == ':') {
                String filename = userIn.split(" ").length > 1 ? userIn.split(" ")[1] : null;
                switch (userIn.split(" ")[0]) {
                    case ":exit":
                    case ":e": {
                        done = true;
                        break;
                    }
                    case ":help":
                    case ":h": {
                        printHelp();
                        break;
                    }
                    case ":reset":
                    case ":r": {
                        typeInfo = new TypeInfo();
                        symbolTable = new SymbolTable();
                        symbolTable.pushEnvironment();
                        globalFrame = new VMFrame("global", 0);
                        vm = new VM();
                        generator = new REPLCodeGenerator(typeInfo, vm, globalFrame);
                        break;
                    }
                    case ":show":
                    case ":s": {
                        System.out.println(symbolTable);
                        break;
                    }
                    case ":prev":
                    case ":p": {
                        runLastCommand = true;
                        break;
                    }
                    case ":load":
                    case ":l": {
                        if (filename != null) {
                            try {
                                System.out.println(new java.io.File(".").getCanonicalPath());
                                io = new FileInputStream(filename);
                                loadFile = true;
                            } catch (IOException e) {
                                System.out.println("File: '" + filename + "' not found");
                                loadFile = false;
                                io = null;
                            }
                            filename = null;
                        } else {
                            System.out.println("File name was not provided");
                        }
                    break;
                    }
                    default: {
                        System.out.println("Invalid REPL Command");
                        printHelp();
                    }
                }
            } else {
                try {
                    evaluateInput(userIn);
                } catch (MyPLException e) {
                    System.err.println(e.getMessage());
                }
                
            }
            if (!runLastCommand && !loadFile) {
                lastCommand = userIn;
            }
        }
    }

    private void evaluateInput(String userIn) throws MyPLException {
        try {
            tokenIn = new ByteArrayInputStream(userIn.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        Lexer lexer = new Lexer(tokenIn);
        REPLASTParser parser = new REPLASTParser(lexer);
        REPLProgram program = parser.parse();
        StaticChecker staticChecker = new StaticChecker(typeInfo);
        staticChecker.setSymbolTable(symbolTable);
        program.accept(staticChecker);
        program.accept(generator);
        vm.add(generator.getGlobalFrame());
        vm.runREPL();
        // Then use the codeGenerator to generate the code and send it to the VM like normal
    }

    private void printHelp() {
        System.out.println("Here are the MyPL REPL functions:");
        System.out.println("Commands can be shortened to their first letter i.e. :h");
        System.out.println("\t:exit    => Exits the REPL");
        System.out.println("\t:help    => Show the help menu");
        System.out.println("\t:reset   => Resets the type environment");
        System.out.println("\t:show => Shows declared types, functions, and variables");
        System.out.println("\t:prev    => Runs previous input to the REPL");
        System.out.println("\t:load    => loads the given file into the REPL");
    }

    private int charsInString(String str, char c) {
        return str.length() - str.replaceAll("[" + c + "]", "").length();
    }
}
