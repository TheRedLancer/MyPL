/* 
 * File: Parser.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: Parses MyPL code and throws an error if syntactially invalid
 */


public class Parser {

  private Lexer lexer = null; 
  private Token currToken = null;
  private final boolean DEBUG = false;

  
  // constructor
  public Parser(Lexer lexer) {
    this.lexer = lexer;
  }

  // do the parse
  public void parse() throws MyPLException
  {
    // <program> ::= (<tdecl> | <fdecl>)*
    advance();
    while (!match(TokenType.EOS)) {
      if (match(TokenType.TYPE))
        tdecl();
      else
        fdecl();
    }
    advance(); // eat the EOS token
  }

  
  //------------------------------------------------------------ 
  // Helper Functions
  //------------------------------------------------------------

  // get next token
  private void advance() throws MyPLException {
    currToken = lexer.nextToken();
  }

  // advance if current token is of given type, otherwise error
  private void eat(TokenType t, String msg) throws MyPLException {
    if (match(t))
      advance();
    else
      error(msg);
  }

  // true if current token is of type t
  private boolean match(TokenType t) {
    return currToken.type() == t;
  }
  
  // throw a formatted parser error
  private void error(String msg) throws MyPLException {
    String s = msg + ", found '" + currToken.lexeme() + "' ";
    s += "at line " + currToken.line();
    s += ", column " + currToken.column();
    throw MyPLException.ParseError(s);
  }

  // output a debug message (if DEBUG is set)
  private void debug(String msg) {
    if (DEBUG)
      System.out.println("[debug]: " + msg);
  }

  // return true if current token is a (non-id) primitive type
  private boolean isPrimitiveType() {
    return match(TokenType.INT_TYPE) || match(TokenType.DOUBLE_TYPE) ||
      match(TokenType.BOOL_TYPE) || match(TokenType.CHAR_TYPE) ||
      match(TokenType.STRING_TYPE);
  }

  // return true if current token is a (non-id) primitive value
  private boolean isPrimitiveValue() {
    return match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }
    
  // return true if current token starts an expression
  private boolean isExpr() {
    return match(TokenType.NOT) || match(TokenType.LPAREN) ||
      match(TokenType.NIL) || match(TokenType.NEW) ||
      match(TokenType.ID) || match(TokenType.NEG) ||
      match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }

  private boolean isOperator() {
    return match(TokenType.PLUS) || match(TokenType.MINUS) ||
      match(TokenType.DIVIDE) || match(TokenType.MULTIPLY) ||
      match(TokenType.MODULO) || match(TokenType.AND) ||
      match(TokenType.OR) || match(TokenType.EQUAL) ||
      match(TokenType.LESS_THAN) || match(TokenType.GREATER_THAN) ||
      match(TokenType.LESS_THAN_EQUAL) || match(TokenType.GREATER_THAN_EQUAL) ||
      match(TokenType.NOT_EQUAL);
  }

  
  //------------------------------------------------------------
  // Recursive Descent Functions 
  //------------------------------------------------------------


  /* TODO: Add the recursive descent functions below */
  private void tdecl() throws MyPLException {
    //<tdecl> ::= TYPE ID LBRACE <vdecls> RBRACE
    eat(TokenType.TYPE, "expecting TYPE");
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.LBRACE, "expecting LBRACE");
    vdecls();
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  private void vdecls() throws MyPLException {
    // <vdecls> ::= ( <vdecl_stmt> )∗
    while (!match(TokenType.RBRACE)) {
      vdecl_stmt();
    }
  }

  private void fdecl() throws MyPLException {
    // <fdecl> ::= FUN ( <dtype> | VOID ) ID LPAREN <params> RPAREN LBRACE <stmts> RBRACE
    eat(TokenType.FUN, "expecting FUN");
    if (match(TokenType.VOID_TYPE)) {
      advance();
    } else {
      dtype();
    }
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.LPAREN, "expecting LPAREN");
    params();
    eat(TokenType.RPAREN, "expecting RPAREN");
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  private void params() throws MyPLException {
    // <params> ::= <dtype> ID ( COMMA <dtype> ID )∗ | <empty>
    if (!match(TokenType.RPAREN)) { // if the next token is a closing paren, there are no parameters
      dtype();
      eat(TokenType.ID, "expecting ID");
      while (match(TokenType.COMMA)) {
        eat(TokenType.COMMA, "expecting COMMA");
        dtype();
        eat(TokenType.ID, "expecting ID");
      }
    }
  }

  private void dtype() throws MyPLException {
    // <dtype> ::= INT_TYPE | DOUBLE_TYPE | BOOL_TYPE | CHAR_TYPE | STRING_TYPE | ID
    if (match(TokenType.ID) || isPrimitiveType()) {
      advance();
    } else {
      error("expecting ID or Primitive Type");
    }
  }

  private void stmts() throws MyPLException {
    // <stmts> ::= ( <stmt> )∗
    while (!match(TokenType.RBRACE)) {
      stmt();
    }
  }

  private void stmt() throws MyPLException {
    // <stmt> ::= <vdecl_stmt> | <assign_stmt> | <cond_stmt> | <while_stmt> | <for_stmt> | <call_expr> | <ret_stmt> | <delete_stmt>
    if (match(TokenType.VAR)) {
      vdecl_stmt();
    } else if (match(TokenType.ID)) {
      // <assign_stmt> or <call_expr>
      Token holdingToken = currToken;
      advance();
      if (match(TokenType.LPAREN)) {
        call_expr();
      } else {
        assign_stmt();
      }
    } else if (match(TokenType.IF)) {
      cond_stmt();
    } else if (match(TokenType.WHILE)) {
      while_stmt();
    } else if (match(TokenType.FOR)) {
      for_stmt();
    } else if (match(TokenType.RETURN)) {
      ret_stmt();
    } else if (match(TokenType.DELETE)) {
      delete_stmt();
    } else {
      error("Invalid statement");
    }
  }

  private void vdecl_stmt() throws MyPLException {
    // <vdecl_stmt> ::= VAR ( <dtype> | <empty> ) ID ASSIGN <expr>
    eat(TokenType.VAR, "expecting VAR");
    if (isPrimitiveType()) {
      advance();
      eat(TokenType.ID, "expecting ID");
    } else if (match(TokenType.ID)) {
      eat(TokenType.ID, "expecting ID");
      if (match(TokenType.ID)) {
        eat(TokenType.ID, "expecting ID");
      }
    }
    eat(TokenType.ASSIGN, "expecting ASSIGN");
    expr();
  }

  private void assign_stmt() throws MyPLException {
    // <assign_stmt> ::= <lvalue> ASSIGN <expr>
    lvalue();
    eat(TokenType.ASSIGN, "expecting ASSIGN");
    expr();
  }

  private void lvalue() throws MyPLException {
    // <lvalue> ::= ID ( DOT ID )∗
    while (match(TokenType.DOT)) {
      eat(TokenType.DOT, "expecting DOT");
      eat(TokenType.ID, "expecting ID");
    }
  }

  private void cond_stmt() throws MyPLException {
    // <cond_stmt> ::= IF <expr> LBRACE <stmts> RBRACE <condt>
    eat(TokenType.IF, "expecting IF");
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE");
    condt();
  }

  private void condt() throws MyPLException {
    // <condt> ::= ELIF <expr> LBRACE <stmts> RBRACE <condt> | ELSE LBRACE <stmts> RBRACE | <empty>
    if (match(TokenType.ELIF)) {
      eat(TokenType.ELIF, "expecting ELIF");
      expr();
      eat(TokenType.LBRACE, "expecting LBRACE");
      stmts();
      eat(TokenType.RBRACE, "expecting RBRACE");
      condt();
    } else if (match(TokenType.ELSE)) {
      eat(TokenType.ELSE, "expecting ELSE");
      eat(TokenType.LBRACE, "expecting LBRACE");
      stmts();
      eat(TokenType.RBRACE, "expecting RBRACE");
    }
  }

  private void while_stmt() throws MyPLException {
    // <while_stmt> ::= WHILE <expr> LBRACE <stmts> RBRACE
    eat(TokenType.WHILE, "expecting WHILE");
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  private void for_stmt() throws MyPLException {
    // <for_stmt> ::= FOR ID FROM <expr> ( UPTO | DOWNTO ) <expr> LBRACE <stmts> RBRACE
    eat(TokenType.FOR, "expecting for");
    eat(TokenType.ID, "expecting ID");
    eat(TokenType.FROM, "expecting FROM");
    expr();
    if (match(TokenType.UPTO) || match(TokenType.DOWNTO)) {
      advance();
    } else {
      error("expecting UPTO or DOWNTO");
    }
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE");
  }

  private void call_expr() throws MyPLException {
    // <call_expr> ::= ID LPAREN <args> RPAREN
    eat(TokenType.LPAREN, "expecting LPAREN");
    args();
    eat(TokenType.RPAREN, "expecting RPAREN");
  }

  private void args() throws MyPLException {
    // <args> ::= <expr> ( COMMA <expr> )∗ | <empty>
    if (!match(TokenType.RPAREN)) {
      expr();
      while (match(TokenType.COMMA)) {
        eat(TokenType.COMMA, "expecting COMMA");
        expr();
      }
    }
  }

  private void ret_stmt() throws MyPLException {
    // <ret_stmt> ::= RETURN ( <expr> | <empty> )
    eat(TokenType.RETURN, "expecting RETURN");
    if (!match(TokenType.RBRACE)) {
      expr();
    }
  }

  private void delete_stmt() throws MyPLException {
    // <delete_stmt> ::= DELETE ID
    eat(TokenType.DELETE, "expecting DELETE");
    eat(TokenType.ID, "expecting ID");
  }

  private void expr() throws MyPLException {
    // <expr> ::= ( <rvalue> | NOT <expr> | LPAREN <expr> RPAREN ) ( <operator> <expr> | <empty> )
    if (match(TokenType.LPAREN)) {
      eat(TokenType.LPAREN, "expecting LPAREN");
      expr();
      eat(TokenType.RPAREN, "expecting RPAREN");
    } else if (match(TokenType.NOT)) {
      eat(TokenType.NOT, "expecting NOT");
      expr();
    } else {
      rvalue();
    }
    if (isOperator()) {
      advance();
      expr();
    }
  }

  // I don't think I need this
  // private void operator() throws MyPLException {
  //   // <operator> ::= PLUS | MINUS | DIVIDE ...

  // }

  private void rvalue() throws MyPLException {
    // <rvalue> ::= <pval> | NIL | NEW ID | <idrval> | <call_expr> | NEG <expr>
    if (match(TokenType.NIL)) {
      advance();
    } else if (match(TokenType.NEW)) {
      advance();
      eat(TokenType.ID, "expecting ID");
    } else if (match(TokenType.NEG)) {
      advance();
      expr();
    } else if (isPrimitiveValue()) {
      advance();
    } else {
      Token holdingToken = currToken;
      eat(TokenType.ID, "expecting ID");
      if (match(TokenType.LPAREN)) {
        call_expr();
      } else {
        idrval();
      }
    }
  }

  // I don't think I need this either
  // private void pval() throws MyPLException {
  //   // <pval> ::= INT_VAL | DOUBLE_VAL | BOOL_VAL | CHAR_VAL | STRING_VAL
  // }

  private void idrval() throws MyPLException {
    // <idrval> ::= ID ( DOT ID )*
    while (match(TokenType.DOT)) {
      eat(TokenType.DOT, "expecting DOT");
      eat(TokenType.ID, "expecting ID");
    }
  }

  
}
