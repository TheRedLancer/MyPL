/*
 * File: PrintVisitor.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: Pretty Printer for MyPL
 */

import java.io.PrintStream;
import java.util.List;


public class PrintVisitor implements Visitor {

  // output stream for printing
  protected PrintStream out;
  // current indent level (number of spaces)
  private int indent = 0;
  // indentation amount
  private final int INDENT_AMT = 2;
  
  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private String getIndent() {
    return " ".repeat(indent);
  }

  private void incIndent() {
    indent += INDENT_AMT;
  }

  private void decIndent() {
    indent -= INDENT_AMT;
  }

  //------------------------------------------------------------
  // VISITOR FUNCTIONS
  //------------------------------------------------------------

  // Hint: To help deal with call expressions, which can be statements
  // or expressions, statements should not indent themselves and add
  // newlines. Instead, the function asking statements to print
  // themselves should add the indent and newlines.
  

  // constructor
  public PrintVisitor(PrintStream printStream) {
    out = printStream;
  }

  
  // top-level nodes

  @Override
  public void visit(Program node) throws MyPLException {
    // print type decls first
    for (TypeDecl d : node.tdecls)
      d.accept(this);
    // print function decls second
    for (FunDecl d : node.fdecls)
      d.accept(this);
  }



  // TODO: Finish the rest of the visitor functions ...

  @Override
  public void visit(TypeDecl node) throws MyPLException {
    // TODO Auto-generated method stub
    out.print("type " + node.typeName.lexeme() + " {\n");
    incIndent();
    for (VarDeclStmt vStmt: node.vdecls) {
      out.print(getIndent());
      vStmt.accept(this);
      out.print("\n");
    }
    decIndent();
    out.print("}\n\n");
  }

  @Override
  public void visit(FunDecl node) throws MyPLException {
    out.print("fun " + node.returnType.lexeme() + " " + node.funName.lexeme() + "(");
    if (node.params.size() >= 1) {
      out.print(node.params.get(0).paramType.lexeme() + " " + node.params.get(0).paramName.lexeme());
      for (int i = 1; i < node.params.size(); i++) {
        out.print(", " + node.params.get(i).paramType.lexeme() + " " + node.params.get(i).paramName.lexeme());
      }
    }
    out.print(") {\n");
    printBlock(node.stmts);
    out.print("}\n\n");
  }

  @Override
  public void visit(VarDeclStmt node) throws MyPLException {
    out.print("var " + ((node.typeName != null) ? node.typeName.lexeme() + " " : "") + node.varName.lexeme() + " = ");
    node.expr.accept(this);
  }

  @Override
  public void visit(AssignStmt node) throws MyPLException {
    out.print(node.lvalue.get(0).lexeme());
    for (int i = 1; i < node.lvalue.size(); i++) {
      out.print("." + node.lvalue.get(i).lexeme());
    }
    out.print(" = ");
    node.expr.accept(this);
  }

  @Override
  public void visit(CondStmt node) throws MyPLException {
    out.print("if ");
    node.ifPart.cond.accept(this);
    out.print(" {\n");
    printBlock(node.ifPart.stmts);
    out.print(getIndent() + "}");
    // if no elifs and no else, print a new line
    if ((!node.elifs.isEmpty() || node.elseStmts != null)) {
      out.print("\n");
    }
    for (BasicIf basicIf : node.elifs) {
      out.print(getIndent() + "elif ");
      basicIf.cond.accept(this);
      out.print(" {\n");
      printBlock(basicIf.stmts);
      out.print(getIndent() + "}\n");
    }
    if (node.elseStmts != null) {
      out.print(getIndent() + "else {\n");
      printBlock(node.elseStmts);
      out.print(getIndent() + "}");
    }
  }

  /**
   * Pretty prints a list of MyPL statements
   * @param stmts list of statements to be printed
   * @throws MyPLException
   */
  private void printBlock(List<Stmt> stmts) throws MyPLException {
    incIndent();
    for (Stmt stmt : stmts) {
      out.print(getIndent());
      stmt.accept(this);
      out.print("\n");
    }
    decIndent();
  }

  @Override
  public void visit(WhileStmt node) throws MyPLException {
    out.print("while ");
    node.cond.accept(this);
    out.print(" {\n");
    printBlock(node.stmts);
    out.print(getIndent() + "}");    
  }

  @Override
  public void visit(ForStmt node) throws MyPLException {
    out.print("for " + node.varName.lexeme() + " from ");
    node.start.accept(this);
    out.print(" " + (node.upto ? "upto" : "downto") + " ");
    node.end.accept(this);
    out.print(" {\n");
    printBlock(node.stmts);
    out.print(getIndent() + "}");
  }

  @Override
  public void visit(ReturnStmt node) throws MyPLException {
    out.print("return ");
    node.expr.accept(this);
  }

  @Override
  public void visit(DeleteStmt node) throws MyPLException {
    out.print("delete " + node.varName.lexeme());   
  }

  @Override
  public void visit(CallExpr node) throws MyPLException {
    out.print(node.funName.lexeme() + "(");
    if (node.args.size() >= 1) {
      node.args.get(0).accept(this);
      for (int i = 1; i < node.args.size(); i++) {
        out.print(", ");
        node.args.get(i).accept(this);
      }
    }
    out.print(")");
  }

  @Override
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.value.type() == TokenType.STRING_VAL) {
      out.print("\"" + node.value.lexeme() + "\"");
    } else if (node.value.type() == TokenType.CHAR_VAL) {
      out.print("'" + node.value.lexeme() + "'");
    } else {
      out.print(node.value.lexeme());
    }
  }

  @Override
  public void visit(NewRValue node) throws MyPLException {
    out.print("new " + node.typeName.lexeme());
    
  }

  @Override
  public void visit(IDRValue node) throws MyPLException {
    out.print(node.path.get(0).lexeme());
    for (int i = 1; i < node.path.size(); i++) {
      out.print("." + node.path.get(i).lexeme());
    }
  }

  @Override
  public void visit(NegatedRValue node) throws MyPLException {
    out.print("neg ");
    node.expr.accept(this);
    out.print("");
  }

  @Override
  public void visit(Expr node) throws MyPLException {
    if (node.op != null || node.logicallyNegated) {
      out.print("(");
    }
    out.print((node.logicallyNegated ? "not " : ""));
    node.first.accept(this);
    if (node.op != null) {
      out.print(" " + node.op.lexeme() + " ");
      node.rest.accept(this);
    }
    if (node.op != null || node.logicallyNegated) {
      out.print(")");
    }
  }

  @Override
  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }

  @Override
  public void visit(ComplexTerm node) throws MyPLException {
    // out.print("(");
    node.expr.accept(this);
    // out.print(")");    
  }
  
}
