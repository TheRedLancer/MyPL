/* 
 * File: ASTParser.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: Parser which builds an AST for MyPL as it parses
 */
import java.util.ArrayList;
import java.util.List;


public class ASTParser {

  protected Lexer lexer = null; 
  protected Token currToken = null;
  protected final boolean DEBUG = false;

  /** 
   */
  public ASTParser(Lexer lexer) {
    this.lexer = lexer;
  }

  /**
   */
  public Program parse() throws MyPLException
  {
    // <program> ::= (<tdecl> | <fdecl>)*
    Program progNode = new Program();
    advance();
    while (!match(TokenType.EOS)) {
      if (match(TokenType.TYPE))
        tdecl(progNode);
      else
        fdecl(progNode);
    }
    advance(); // eat the EOS token
    return progNode;
  }

  
  //------------------------------------------------------------ 
  // Helper Functions
  //------------------------------------------------------------

  // get next token
  protected void advance() throws MyPLException {
    currToken = lexer.nextToken();
  }

  // advance if current token is of given type, otherwise error
  protected void eat(TokenType t, String msg) throws MyPLException {
    if (match(t))
      advance();
    else
      error(msg);
  }

  // true if current token is of type t
  protected boolean match(TokenType t) {
    return currToken.type() == t;
  }
  
  // throw a formatted parser error
  protected void error(String msg) throws MyPLException {
    String s = msg + ", found '" + currToken.lexeme() + "' ";
    s += "at line " + currToken.line();
    s += ", column " + currToken.column();
    throw MyPLException.ParseError(s);
  }

  // output a debug message (if DEBUG is set)
  protected void debug(String msg) {
    if (DEBUG)
      System.out.println("[debug]: " + msg);
  }

  // return true if current token is a (non-id) primitive type
  protected boolean isPrimitiveType() {
    return match(TokenType.INT_TYPE) || match(TokenType.DOUBLE_TYPE) ||
      match(TokenType.BOOL_TYPE) || match(TokenType.CHAR_TYPE) ||
      match(TokenType.STRING_TYPE);
  }

  protected boolean isOperator() {
    return match(TokenType.PLUS) || match(TokenType.MINUS) ||
      match(TokenType.DIVIDE) || match(TokenType.MULTIPLY) ||
      match(TokenType.MODULO) || match(TokenType.AND) ||
      match(TokenType.OR) || match(TokenType.EQUAL) ||
      match(TokenType.LESS_THAN) || match(TokenType.GREATER_THAN) ||
      match(TokenType.LESS_THAN_EQUAL) || match(TokenType.GREATER_THAN_EQUAL) ||
      match(TokenType.NOT_EQUAL);
  }

  // return true if current token is a (non-id) primitive value
  protected boolean isPrimitiveValue() {
    return match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }

  
  //------------------------------------------------------------
  // Recursive Descent Functions 
  //------------------------------------------------------------


  // TODO: Add your recursive descent functions from HW-3
  // and extend them to build up the AST

  protected void tdecl(Program progNode) throws MyPLException {
    //<tdecl> ::= TYPE ID LBRACE <vdecls> RBRACE
    TypeDecl tDecl = new TypeDecl();
    eat(TokenType.TYPE, "expecting TYPE");
    tDecl.typeName = currToken;
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.LBRACE, "expecting LBRACE");
    vdecls(tDecl);
    eat(TokenType.RBRACE, "expecting RBRACE");
    progNode.tdecls.add(tDecl);
  }

  protected void vdecls(TypeDecl tDecl) throws MyPLException {
    // <vdecls> ::= ( <vdecl_stmt> )∗
    while (!match(TokenType.RBRACE)) {
      VarDeclStmt varDeclStmt = new VarDeclStmt();
      vdecl_stmt(varDeclStmt);
      tDecl.vdecls.add(varDeclStmt);
    }
  }

  protected void vdecl_stmt(VarDeclStmt vStmt) throws MyPLException {
    // <vdecl_stmt> ::= VAR ( <dtype> | <empty> ) ID ASSIGN <expr>
    eat(TokenType.VAR, "expecting VAR");
    if (isPrimitiveType()) {
      vStmt.typeName = currToken;
      advance();
      vStmt.varName = currToken;
      eat(TokenType.ID, "expecting ID");
    } else if (match(TokenType.ID)) {
      vStmt.varName = currToken;
      eat(TokenType.ID, "expecting ID");
      if (match(TokenType.ID)) {
        vStmt.typeName = vStmt.varName;
        vStmt.varName = currToken;
        eat(TokenType.ID, "expecting ID");
      }
    }
    eat(TokenType.ASSIGN, "expecting ASSIGN");
    Expr expr = new Expr();
    expr(expr);
    vStmt.expr = expr;
  }

  protected void expr(Expr expr) throws MyPLException {
    // <expr> ::= ( <rvalue> | NOT <expr> | LPAREN <expr> RPAREN ) ( <operator> <expr> | <empty> )
    if (match(TokenType.LPAREN)) {
      eat(TokenType.LPAREN, "expecting LPAREN");
      ComplexTerm cTerm = new ComplexTerm();
      cTerm.expr = new Expr();
      expr(cTerm.expr);
      expr.first = cTerm;
      eat(TokenType.RPAREN, "expecting RPAREN");
    } else if (match(TokenType.NOT)) {
      eat(TokenType.NOT, "expecting NOT");
      expr.logicallyNegated = true;
      ComplexTerm cTerm = new ComplexTerm();
      cTerm.expr = new Expr();
      expr(cTerm.expr);
      expr.first = cTerm;
    } else {
      SimpleTerm sTerm = new SimpleTerm();
      rvalue(sTerm);
      expr.first = sTerm;
    }
    if (isOperator()) {
      expr.op = currToken;
      advance();
      Expr rest = new Expr();
      expr(rest);
      expr.rest = rest;
    }
  }

  protected void rvalue(SimpleTerm sTerm) throws MyPLException {
    // <rvalue> ::= <pval> | NIL | NEW ID | <idrval> | <call_expr> | NEG <expr>
    if (match(TokenType.NIL)) {
      SimpleRValue sRValue = new SimpleRValue();
      sRValue.value = currToken;
      advance();
      sTerm.rvalue = sRValue;
    } else if (match(TokenType.NEW)) {
      NewRValue nRValue = new NewRValue();
      advance();
      nRValue.typeName = currToken;
      eat(TokenType.ID, "expecting ID");
      sTerm.rvalue = nRValue;
    } else if (match(TokenType.NEG)) {
      advance();
      NegatedRValue nRValue = new NegatedRValue();
      Expr expr = new Expr();
      expr(expr);
      nRValue.expr = expr;
      sTerm.rvalue = nRValue;
    } else if (isPrimitiveValue()) {
      SimpleRValue sRValue = new SimpleRValue();
      sRValue.value = currToken;
      advance();
      sTerm.rvalue = sRValue;
    } else {
      Token holdingToken = currToken;
      eat(TokenType.ID, "expecting ID");
      if (match(TokenType.LPAREN)) {
        CallExpr cExpr = new CallExpr();
        cExpr.funName = holdingToken;
        call_expr(cExpr);
        sTerm.rvalue = cExpr;
      } else {
        IDRValue idrValue = new IDRValue();
        idrValue.path.add(holdingToken);
        idrval(idrValue);
        sTerm.rvalue = idrValue;
      }
    }
  }

  protected void call_expr(CallExpr cExpr) throws MyPLException {
    // <call_expr> ::= ID LPAREN <args> RPAREN
    eat(TokenType.LPAREN, "expecting LPAREN");
    args(cExpr);
    eat(TokenType.RPAREN, "expecting RPAREN");
  }

  protected void args(CallExpr cExpr) throws MyPLException {
    // <args> ::= <expr> ( COMMA <expr> )∗ | <empty>
    Expr expr;
    if (!match(TokenType.RPAREN)) {
      expr = new Expr();
      expr(expr);
      cExpr.args.add(expr);
      while (match(TokenType.COMMA)) {
        eat(TokenType.COMMA, "expecting COMMA");
        expr = new Expr();
        expr(expr);
        cExpr.args.add(expr);
      }
    }
  }

  protected void idrval(IDRValue idrValue) throws MyPLException {
    // <idrval> ::= ID ( DOT ID )*
    while (match(TokenType.DOT)) {
      eat(TokenType.DOT, "expecting DOT");
      idrValue.path.add(currToken);
      eat(TokenType.ID, "expecting ID");
    }
  }

  protected void fdecl(Program progNode) throws MyPLException {
    // <fdecl> ::= FUN ( <dtype> | VOID ) ID LPAREN <params> RPAREN LBRACE <stmts> RBRACE
    FunDecl fDeclNode = new FunDecl();
    eat(TokenType.FUN, "expecting FUN");
    fDeclNode.returnType = currToken;
    if (match(TokenType.VOID_TYPE)) {
      advance();
    } else {
      dtype();
    }
    fDeclNode.funName = currToken;
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.LPAREN, "expecting LPAREN");
    params(fDeclNode);
    eat(TokenType.RPAREN, "expecting RPAREN");
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts(fDeclNode.stmts);
    eat(TokenType.RBRACE, "expecting RBRACE");
    progNode.fdecls.add(fDeclNode);
  }

  protected void dtype() throws MyPLException {
    // <dtype> ::= INT_TYPE | DOUBLE_TYPE | BOOL_TYPE | CHAR_TYPE | STRING_TYPE | ID
    if (match(TokenType.ID) || isPrimitiveType()) {
      advance();
    } else {
      error("expecting ID or Primitive Type");
    }
  }

  protected void params(FunDecl fDeclNode) throws MyPLException {
    // <params> ::= <dtype> ID ( COMMA <dtype> ID )∗ | <empty>
    FunParam fParam;
    if (!match(TokenType.RPAREN)) { // if the next token is a closing paren, there are no parameters
      fParam = new FunParam();
      fParam.paramType = currToken;
      dtype();
      fParam.paramName = currToken;
      eat(TokenType.ID, "expecting ID");
      fDeclNode.params.add(fParam);
      while (match(TokenType.COMMA)) {
        eat(TokenType.COMMA, "expecting COMMA");
        fParam = new FunParam();
        fParam.paramType = currToken;
        dtype();
        fParam.paramName = currToken;
        eat(TokenType.ID, "expecting ID");
        fDeclNode.params.add(fParam);
      }
    }
  }

  protected void stmts(List<Stmt> stmts) throws MyPLException {
    // <stmts> ::= ( <stmt> )∗
    while (!match(TokenType.RBRACE)) {
      stmt(stmts);
    }
  }

  protected void stmt(List<Stmt> stmts) throws MyPLException {
    // <stmt> ::= <vdecl_stmt> | <assign_stmt> | <cond_stmt> | <while_stmt> | <for_stmt> | <call_expr> | <ret_stmt> | <delete_stmt>
    if (match(TokenType.VAR)) {
      VarDeclStmt varDeclStmt = new VarDeclStmt();
      vdecl_stmt(varDeclStmt);
      stmts.add(varDeclStmt);
    } else if (match(TokenType.ID)) {
      // <assign_stmt> or <call_expr>
      Token holdingToken = currToken;
      advance();
      if (match(TokenType.LPAREN)) {
        CallExpr cExpr = new CallExpr();
        cExpr.funName = holdingToken;
        call_expr(cExpr);
        stmts.add(cExpr);
      } else {
        AssignStmt aStmt = new AssignStmt();
        aStmt.lvalue.add(holdingToken);
        assign_stmt(aStmt);
        stmts.add(aStmt);
      }
    } else if (match(TokenType.IF)) {
      CondStmt cStmt = new CondStmt();
      cond_stmt(cStmt);
      stmts.add(cStmt);
    } else if (match(TokenType.WHILE)) {
      WhileStmt wStmt = new WhileStmt();
      while_stmt(wStmt);
      stmts.add(wStmt);
    } else if (match(TokenType.FOR)) {
      ForStmt fStmt = new ForStmt();
      for_stmt(fStmt);
      stmts.add(fStmt);
    } else if (match(TokenType.RETURN)) {
      ReturnStmt rStmt = new ReturnStmt();
      ret_stmt(rStmt);
      stmts.add(rStmt);
    } else if (match(TokenType.DELETE)) {
      DeleteStmt dStmt = new DeleteStmt();
      delete_stmt(dStmt);
      stmts.add(dStmt);
    } else {
      error("Invalid statement");
    }
  }

  protected void assign_stmt(AssignStmt aStmt) throws MyPLException {
    // <assign_stmt> ::= <lvalue> ASSIGN <expr>
    lvalue(aStmt);
    eat(TokenType.ASSIGN, "expecting ASSIGN");
    Expr expr = new Expr();
    expr(expr);
    aStmt.expr = expr;
  }

  protected void lvalue(AssignStmt aStmt) throws MyPLException {
    // <lvalue> ::= ID ( DOT ID )∗
    // Already removed first ID when this is called
    while (match(TokenType.DOT)) {
      eat(TokenType.DOT, "expecting DOT");
      aStmt.lvalue.add(currToken);
      eat(TokenType.ID, "expecting ID");
    }
  }

  protected void cond_stmt(CondStmt cStmt) throws MyPLException {
    // <cond_stmt> ::= IF <expr> LBRACE <stmts> RBRACE <condt>
    BasicIf basicIf = new BasicIf();
    eat(TokenType.IF, "expecting IF");
    Expr expr = new Expr();
    expr(expr);
    basicIf.cond = expr;
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts(basicIf.stmts);
    eat(TokenType.RBRACE, "expecting RBRACE");
    cStmt.ifPart = basicIf;
    condt(cStmt);
  }

  protected void condt(CondStmt cStmt) throws MyPLException {
    // <condt> ::= ELIF <expr> LBRACE <stmts> RBRACE <condt> | ELSE LBRACE <stmts> RBRACE | <empty>
    if (match(TokenType.ELIF)) {
      eat(TokenType.ELIF, "expecting ELIF");
      // Same as BasicIf excpet add to the elif section
      BasicIf basicIf = new BasicIf();
      Expr expr = new Expr();
      expr(expr);
      basicIf.cond = expr;
      eat(TokenType.LBRACE, "expecting LBRACE");
      stmts(basicIf.stmts);
      eat(TokenType.RBRACE, "expecting RBRACE");
      cStmt.elifs.add(basicIf);
      condt(cStmt);
    } else if (match(TokenType.ELSE)) {
      eat(TokenType.ELSE, "expecting ELSE");
      eat(TokenType.LBRACE, "expecting LBRACE");
      cStmt.elseStmts = new ArrayList<>();
      stmts(cStmt.elseStmts);
      eat(TokenType.RBRACE, "expecting RBRACE");
    }
  }

  protected void while_stmt(WhileStmt wStmt) throws MyPLException {
    // <while_stmt> ::= WHILE <expr> LBRACE <stmts> RBRACE
    eat(TokenType.WHILE, "expecting WHILE");
    Expr expr = new Expr();
    expr(expr);
    wStmt.cond = expr;
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts(wStmt.stmts);
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  protected void for_stmt(ForStmt fStmt) throws MyPLException {
    // <for_stmt> ::= FOR ID FROM <expr> ( UPTO | DOWNTO ) <expr> LBRACE <stmts> RBRACE
    eat(TokenType.FOR, "expecting for");
    fStmt.varName = currToken;
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.FROM, "expecting FROM");
    Expr start = new Expr();
    expr(start);
    fStmt.start = start;
    if (match(TokenType.UPTO) || match(TokenType.DOWNTO)) {
      if (match(TokenType.DOWNTO)) {
        fStmt.upto = false;
      }
      advance();
    } else {
      error("expecting UPTO or DOWNTO");
    }
    Expr end = new Expr();
    expr(end);
    fStmt.end = end;
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts(fStmt.stmts);
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  protected void ret_stmt(ReturnStmt rStmt) throws MyPLException {
    // <ret_stmt> ::= RETURN ( <expr> | <empty> )
    eat(TokenType.RETURN, "expecting RETURN");
    if (!match(TokenType.RBRACE)) {
      Expr e = new Expr();
      expr(e);
      rStmt.expr = e;
    }
  }

  protected void delete_stmt(DeleteStmt dStmt) throws MyPLException {
    // <delete_stmt> ::= DELETE ID
    eat(TokenType.DELETE, "expecting DELETE");
    dStmt.varName = currToken;
    eat(TokenType.ID, "expecting ID");
  }
  
}