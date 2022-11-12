/*
 * File: StaticChecker.java
 * Date: Spring 2022
 * Auth: 
 * Desc: 
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

// NOTE: Some of the following are filled in, some partly filled in,
// and most left for you to fill in. The helper functions are provided
// for you to use as needed. 

public class StaticChecker implements Visitor {

  // the symbol table
  private SymbolTable symbolTable = new SymbolTable();
  // the current expression type
  private String currType = null;
  // the program's user-defined (record) types and function signatures
  private TypeInfo typeInfo = null;

  private boolean returnFlag = false;

  // --------------------------------------------------------------------
  // helper functions:
  // --------------------------------------------------------------------

  // generate an error
  private void error(String msg, Token token) throws MyPLException {
    String s = msg;
    if (token != null)
      s += " near line " + token.line() + ", column " + token.column();
    throw MyPLException.StaticError(s);
  }

  // return all valid types
  // assumes user-defined types already added to symbol table
  private List<String> getValidTypes() {
    List<String> types = new ArrayList<>();
    types.addAll(Arrays.asList("int", "double", "bool", "char", "string",
        "void"));
    for (String type : typeInfo.types())
      if (symbolTable.get(type).equals("type"))
        types.add(type);
    return types;
  }

  // return the build in function names
  private List<String> getBuiltinFunctions() {
    return Arrays.asList("print", "read", "length", "get", "stoi",
        "stod", "itos", "itod", "dtos", "dtoi");
  }

  // check if given token is a valid function signature return type
  private void checkReturnType(Token typeToken) throws MyPLException {
    if (!getValidTypes().contains(typeToken.lexeme())) {
      String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
      error(msg, typeToken);
    }
  }

  // helper to check if the given token is a valid parameter type
  private void checkParamType(Token typeToken) throws MyPLException {
    if (typeToken.equals("void"))
      error("'void' is an invalid parameter type", typeToken);
    else if (!getValidTypes().contains(typeToken.lexeme())) {
      String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
      error(msg, typeToken);
    }
  }

  // helpers to get first token from an expression for calls to error

  private Token getFirstToken(Expr expr) {
    return getFirstToken(expr.first);
  }

  private Token getFirstToken(ExprTerm term) {
    if (term instanceof SimpleTerm)
      return getFirstToken(((SimpleTerm) term).rvalue);
    else
      return getFirstToken(((ComplexTerm) term).expr);
  }

  private Token getFirstToken(RValue rvalue) {
    if (rvalue instanceof SimpleRValue)
      return ((SimpleRValue) rvalue).value;
    else if (rvalue instanceof NewRValue)
      return ((NewRValue) rvalue).typeName;
    else if (rvalue instanceof IDRValue)
      return ((IDRValue) rvalue).path.get(0);
    else if (rvalue instanceof CallExpr)
      return ((CallExpr) rvalue).funName;
    else
      return getFirstToken(((NegatedRValue) rvalue).expr);
  }

  public void setSymbolTable(SymbolTable sTable) {
    symbolTable = sTable;
  }

  // ---------------------------------------------------------------------
  // constructor
  // --------------------------------------------------------------------

  public StaticChecker(TypeInfo typeInfo) {
    this.typeInfo = typeInfo;
  }

  // --------------------------------------------------------------------
  // top-level nodes
  // --------------------------------------------------------------------

  public void visit(REPLProgram node) throws MyPLException {
    // (1) add each user-defined type name to the symbol table and to
    // the list of rec types, check for duplicate names
    for (TypeDecl tdecl : node.tdecls) {
      String t = tdecl.typeName.lexeme();
      if (symbolTable.nameExists(t))
        error("type '" + t + "' already defined", tdecl.typeName);
      // add as a record type to the symbol table
      symbolTable.add(t, "type");
      // add initial type info (rest added by TypeDecl visit function)
      typeInfo.add(t);
    }

    // (2) add each function name and signature to the symbol
    // table check for duplicate names
    for (FunDecl fdecl : node.fdecls) {
      String funName = fdecl.funName.lexeme();
      // make sure not redefining built-in functions
      if (getBuiltinFunctions().contains(funName)) {
        String m = "cannot redefine built in function " + funName;
        error(m, fdecl.funName);
      }

      // check if function already exists
      if (symbolTable.nameExists(funName))
        error("function '" + funName + "' already defined", fdecl.funName);

      // make sure the return type is a valid type
      checkReturnType(fdecl.returnType);
      // add to the symbol table as a function
      symbolTable.add(funName, "fun");
      // add to typeInfo
      typeInfo.add(funName);

      for (FunParam funParam : fdecl.params) {
        checkParamType(funParam.paramType);
        if (typeInfo.get(funName, funParam.paramName.lexeme()) != null) {
          error("Cannot have 2 parameters with the same name", funParam.paramName);
        }
        typeInfo.add(funName, funParam.paramName.lexeme(), funParam.paramType.lexeme());
      }
      // add the return type
      typeInfo.add(funName, "return", fdecl.returnType.lexeme());
    }

    // check each type and function
    for (TypeDecl tdecl : node.tdecls)
      tdecl.accept(this);
    for (FunDecl fdecl : node.fdecls)
      fdecl.accept(this);
    for (Stmt stmt : node.stmts) {
      stmt.accept(this);
    }
  }


  public void visit(Program node) throws MyPLException {
    // push the "global" environment
    symbolTable.pushEnvironment();

    // (1) add each user-defined type name to the symbol table and to
    // the list of rec types, check for duplicate names
    for (TypeDecl tdecl : node.tdecls) {
      String t = tdecl.typeName.lexeme();
      if (symbolTable.nameExists(t))
        error("type '" + t + "' already defined", tdecl.typeName);
      // add as a record type to the symbol table
      symbolTable.add(t, "type");
      // add initial type info (rest added by TypeDecl visit function)
      typeInfo.add(t);
    }

    // (2) add each function name and signature to the symbol
    // table check for duplicate names
    for (FunDecl fdecl : node.fdecls) {
      String funName = fdecl.funName.lexeme();
      // make sure not redefining built-in functions
      if (getBuiltinFunctions().contains(funName)) {
        String m = "cannot redefine built in function " + funName;
        error(m, fdecl.funName);
      }
      // check if function already exists
      if (symbolTable.nameExists(funName))
        error("function '" + funName + "' already defined", fdecl.funName);

      // TODO: Build the function param names and signature.

      // make sure the return type is a valid type
      checkReturnType(fdecl.returnType);
      // add to the symbol table as a function
      symbolTable.add(funName, "fun");
      // add to typeInfo
      typeInfo.add(funName);

      for (FunParam funParam : fdecl.params) {
        checkParamType(funParam.paramType);
        if (typeInfo.get(funName, funParam.paramName.lexeme()) != null) {
          error("Cannot have 2 parameters with the same name", funParam.paramName);
        }
        typeInfo.add(funName, funParam.paramName.lexeme(), funParam.paramType.lexeme());
      }
      // add the return type
      typeInfo.add(funName, "return", fdecl.returnType.lexeme());
    }

    if (!symbolTable.nameExists("main")) {
      error("no main function defined", null);
    }
    if (typeInfo.components("main").size() != 1) {
      error("main function has arguments", null);
    }
    if (typeInfo.get("main", "return").compareTo("void") != 0) {
      error("main function does not have void return type", null);
    }

    // check each type and function
    for (TypeDecl tdecl : node.tdecls)
      tdecl.accept(this);
    for (FunDecl fdecl : node.fdecls)
      fdecl.accept(this);

    // all done, pop the global table
    symbolTable.popEnvironment();
  }

  public void visit(TypeDecl node) throws MyPLException {
    symbolTable.pushEnvironment();
    for (VarDeclStmt vDecl : node.vdecls) {
      vDecl.accept(this);
      typeInfo.add(node.typeName.lexeme(), vDecl.varName.lexeme(), currType);
    }
    symbolTable.popEnvironment();
  }

  public void visit(FunDecl node) throws MyPLException {
    symbolTable.pushEnvironment();
    for (FunParam funParam : node.params) {
      symbolTable.add(funParam.paramName.lexeme(), funParam.paramType.lexeme());
    }
    for (Stmt stmt : node.stmts) {
      if (!returnFlag) {
        stmt.accept(this);
      }
      if (returnFlag) {
        if (!currType.equals(typeInfo.get(node.funName.lexeme(), "return")) && !currType.equals("void")) {
          error("Return type mismatch", node.returnType);
        }
        break;
      }
    }
    returnFlag = false;
    symbolTable.popEnvironment();
  }

  // --------------------------------------------------------------------
  // statement nodes
  // --------------------------------------------------------------------

  public void visit(VarDeclStmt node) throws MyPLException {
    node.expr.accept(this);
    String expType = currType;
    String varName = node.varName.lexeme();
    if (symbolTable.nameExistsInCurrEnv(varName)) {
      error("A variable wth this name already exists", node.varName);
    }
    if (node.typeName == null && expType == "void") {
      error("Variable type cannot be inferred", node.varName);
    }
    if (node.typeName != null && !node.typeName.lexeme().equals(expType) && !expType.equals("void")) {
      // expression result must match var type
      error("Variable declaration evaluates to mismatched type", node.varName);
    }
    if (node.typeName != null) {
      expType = node.typeName.lexeme();
    }
    symbolTable.add(varName, expType);
    currType = expType;
  }

  public void visit(AssignStmt node) throws MyPLException {
    node.expr.accept(this);
    String expType = currType;
    if (node.lvalue.get(0).lexeme().equals("it")) {
      symbolTable.remove("it");
      symbolTable.add("it", expType);
    }
    getTokenPathType(node.lvalue);
    String lvalueType = currType;
    if (!expType.equals(lvalueType) && !expType.equals("void")) {
      error("Expression does not evaluate to variable type", node.lvalue.get(0));
    }
    if (lvalueType.equals("fun")) {
      error("Function names are not assignable", getFirstToken(node.expr));
    }
  }

  private void getTokenPathType(List<Token> path) throws MyPLException {
    // t.next.next.x = 42
    String userVarName = path.get(0).lexeme();
    // is var defined
    if (!symbolTable.nameExists(userVarName)) {
      error("Variable is not declared", path.get(0));
    }
    // store var type
    currType = symbolTable.get(userVarName);
    if (currType.equals("fun")) {
      error("Function name cannot be accessed as variable", path.get(0));
    }
    if (currType.equals("type")) {
      error("Type names cannot be used as variables", path.get(0));
    }
    if (path.size() > 1) {
      // for each subsequent token
      for (Token token : path.subList(1, path.size())) {
        // check if token is component of stored type
        if (!typeInfo.components(currType).contains(token.lexeme())) {
          error("Type " + currType + " does not have component " + token.lexeme(), token);
        }
        // store that components type
        currType = typeInfo.get(currType, token.lexeme());
      }
    }
  }

  public void visit(CondStmt node) throws MyPLException {
    node.ifPart.cond.accept(this);
    if (!currType.equals("bool")) {
      error("Expression does not evaluate to bool type", getFirstToken(node.ifPart.cond));
    }
    symbolTable.pushEnvironment();
    for (Stmt stmt : node.ifPart.stmts) {
      if (!returnFlag) {
        stmt.accept(this);
      }
    }
    symbolTable.popEnvironment();

    for (BasicIf basicIf : node.elifs) {
      basicIf.cond.accept(this);
      if (!currType.equals("bool")) {
        error("Expression does not evaluate to bool type", getFirstToken(basicIf.cond));
      }
      symbolTable.pushEnvironment();
      for (Stmt stmt : basicIf.stmts) {
        if (!returnFlag) {
          stmt.accept(this);
        }
      }
      symbolTable.popEnvironment();
    }

    if (node.elseStmts != null) {
      symbolTable.pushEnvironment();
      for (Stmt stmt : node.elseStmts) {
        if (!returnFlag) {
          stmt.accept(this);
        }
      }
      symbolTable.popEnvironment();
    }
  }

  public void visit(WhileStmt node) throws MyPLException {
    node.cond.accept(this);
    if (currType != "bool") {
      error("While condition does not evaluate to bool", getFirstToken(node.cond));
    }
    symbolTable.pushEnvironment();
    for (Stmt stmt : node.stmts) {
      if (!returnFlag) {
        stmt.accept(this);
      }
    }
    symbolTable.popEnvironment();
  }

  public void visit(ForStmt node) throws MyPLException {
    // initialize var as type int
    symbolTable.pushEnvironment();
    symbolTable.add(node.varName.lexeme(), "int");
    // start and end are ints
    node.start.accept(this);
    if (!currType.equals("int")) {
      error("For loop range expression does not evaluate to int", getFirstToken(node.start));
    }
    node.end.accept(this);
    if (!currType.equals("int")) {
      error("For loop range expression does not evaluate to int", getFirstToken(node.end));
    }
    for (Stmt stmt : node.stmts) {
      if (!returnFlag) {
        stmt.accept(this);
      }
    }
    symbolTable.popEnvironment();
  }

  public void visit(ReturnStmt node) throws MyPLException {
    if (node.expr == null) {
      currType = "void";
    } else {
      node.expr.accept(this);
    }
    returnFlag = true;
  }

  public void visit(DeleteStmt node) throws MyPLException {
    if (!symbolTable.nameExists(node.varName.lexeme())) {
      error("Variable does not exist", node.varName);
    } else if (!symbolTable.get(node.varName.lexeme()).equals("type")) {
      error("Can only delete UDT", node.varName);
    }
  }

  // ----------------------------------------------------------------------
  // statement and rvalue node
  // ----------------------------------------------------------------------

  private void checkBuiltIn(CallExpr node) throws MyPLException {
    String funName = node.funName.lexeme();
    if (funName.equals("print")) {
      node.args.get(0).accept(this);
      // has to have one argument, any type is allowed
      if (node.args.size() != 1)
        error("print expects one argument", node.funName);
      currType = "void";
    } else if (funName.equals("read")) {
      // no arguments allowed
      if (node.args.size() != 0)
        error("read takes no arguments", node.funName);
      currType = "string";
    } else if (funName.equals("length")) {
      // one string argument
      if (node.args.size() != 1)
        error("length expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("string"))
        error("expecting string in length", getFirstToken(e));
      currType = "int";
    } else if (funName.equals("get")) {
      if (node.args.size() != 2)
        error("get expects two argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("int"))
        error("expecting int in get", getFirstToken(e));
      e = node.args.get(1);
      e.accept(this);
      if (!currType.equals("string")) {
        error("expecting string in get", getFirstToken(e));
      }
      currType = "char";
    } else if (funName.equals("stoi")) {
      typeChange(node, "string", "int", "stoi");
    } else if (funName.equals("stod")) {
      typeChange(node, "string", "double", "stod");
    } else if (funName.equals("itos")) {
      typeChange(node, "int", "string", "itos");
    } else if (funName.equals("itod")) {
      typeChange(node, "int", "double", "itod");
    } else if (funName.equals("dtos")) {
      typeChange(node, "double", "string", "dtos");
    } else if (funName.equals("dtoi")) {
      typeChange(node, "double", "int", "dtoi");
    }
  }

  private void typeChange(CallExpr node, String from, String to, String fName) throws MyPLException {
    if (node.args.size() != 1)
      error(fName + " expects one argument", node.funName);
    Expr e = node.args.get(0);
    e.accept(this);
    if (!currType.equals(from)) {
      error("expecting " + from + " in " + fName, getFirstToken(e));
    }
    currType = to;
  }

  public void visit(CallExpr node) throws MyPLException {
    if (getBuiltinFunctions().contains(node.funName.lexeme())) {
      checkBuiltIn(node);
    } else {
      String funName = node.funName.lexeme();
      if (!symbolTable.nameExists(funName)) {
        error("Function is not defined", node.funName);
      }
      List<String> argVarNames = new ArrayList<>(typeInfo.components(funName));
      if (node.args.size() != argVarNames.size() - 1) {
        error("Incorrect number of arguments", node.funName);
      }
      for (int i = 0; i < node.args.size(); i++) {
        node.args.get(i).accept(this);
        String expectedType = typeInfo.get(funName, argVarNames.get(i));
        if (!currType.equals(expectedType) && !currType.equals("void")) {
          error("Arg type mismatch in fun " + funName + ", expecting " + expectedType + ", but got " + currType,
              getFirstToken(node.args.get(i)));
        }
        // symbolTable.add(argVarNames.get(i), expectedType);
      }
      currType = typeInfo.get(node.funName.lexeme(), "return");
    }
  }

  // ----------------------------------------------------------------------
  // rvalue nodes
  // ----------------------------------------------------------------------

  public void visit(SimpleRValue node) throws MyPLException {
    TokenType tokenType = node.value.type();
    if (tokenType == TokenType.INT_VAL)
      currType = "int";
    else if (tokenType == TokenType.DOUBLE_VAL)
      currType = "double";
    else if (tokenType == TokenType.BOOL_VAL)
      currType = "bool";
    else if (tokenType == TokenType.CHAR_VAL)
      currType = "char";
    else if (tokenType == TokenType.STRING_VAL)
      currType = "string";
    else if (tokenType == TokenType.NIL)
      currType = "void";
  }

  public void visit(NewRValue node) throws MyPLException {
    String newType = node.typeName.lexeme();
    if (!symbolTable.nameExists(newType) || symbolTable.get(newType).equals("fun")) {
      error("Type does not exist", node.typeName);
    }
    currType = newType;
  }

  public void visit(IDRValue node) throws MyPLException {
    getTokenPathType(node.path);
  }

  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
    if (!currType.equals("int") && !currType.equals("double")) {
      error("Negated expression evaluates to " + currType + " not \"int\" or \"double\"", getFirstToken(node));
    }
  }

  // ----------------------------------------------------------------------
  // expression node
  // ----------------------------------------------------------------------

  public void visit(Expr node) throws MyPLException {
    List<TokenType> booleanOps = Arrays.asList(new TokenType[] { TokenType.LESS_THAN, TokenType.LESS_THAN_EQUAL,
        TokenType.GREATER_THAN, TokenType.GREATER_THAN_EQUAL });
    node.first.accept(this);
    List<TokenType> arithmeticTypes = Arrays
        .asList(new TokenType[] { TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE });
    node.first.accept(this);
    if (node.rest == null) {
      // if there is just a first exprTerm
      if (node.logicallyNegated && !currType.equals("bool")) {
        error("Logical negation only valid for bool type", getFirstToken(node.first));
      }
      return;
    }
    String firstType = currType;
    node.rest.accept(this);
    String restType = currType;
    boolean charAdd = (firstType.equals("char") && restType.equals("char"));
    // String/Char
    if (charAdd && node.op.type() == TokenType.PLUS) {
      error("Cannot add two char types", node.op);
    }
    if (node.op.type().equals(TokenType.PLUS) && !charAdd
        && ((firstType.equals("string") && restType.equals("string"))
            || (firstType.equals("char") && restType.equals("string"))
            || (firstType.equals("string") && restType.equals("char")))) {
      currType = "string";
    }

    // Arithmetic Types
    else if (arithmeticTypes.contains(node.op.type())) {
      if (!firstType.equals(restType) || (!firstType.equals("int") && !firstType.equals("double"))) {
        error("arithmetic operations invalid for '" + firstType + "' and '" + restType
            + "' types, both sides must be same type", node.op);
      }
    }

    // Modulo
    else if (node.op.type().equals(TokenType.MODULO)) {
      if (!firstType.equals(restType) || !firstType.equals("int")) {
        error("Modulo is only valid with 2 int", node.op);
      }
    }

    // Equality Operations
    else if (node.op.type().equals(TokenType.EQUAL) || node.op.type().equals(TokenType.NOT_EQUAL)) {
      if (!(firstType.equals(restType) || firstType.equals("void") || restType.equals("void"))) {
        error("Equality operators == and != must have the same type on both sides", node.op);
      } else {
        currType = "bool";
      }
    }

    // Boolean Operators
    else if (booleanOps.contains(node.op.type())) {
      // >,>=, <, <=
      if (!firstType.equals(restType)) {
        error("boolean ops must have same type", node.op);
      } else if (!firstType.equals("int") && !firstType.equals("double") && !firstType.equals("char")
          && !firstType.equals("string")) {
        error("boolean ops are not valid for UDT", node.op);
      } else {
        currType = "bool";
      }
    }

  }

  // ----------------------------------------------------------------------
  // terms
  // ----------------------------------------------------------------------

  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }

  public void visit(ComplexTerm node) throws MyPLException {
    node.expr.accept(this);
  }

}
