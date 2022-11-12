import java.util.ArrayList;
import java.util.List;

public class REPLASTParser extends ASTParser {

    boolean inFun;

    public REPLASTParser(Lexer lexer) {
        super(lexer);
        inFun = false;
    }

    @Override
    protected void advance() throws MyPLException {
      currToken = lexer.nextToken();
      if (match(TokenType.ID) && currToken.lexeme().equals("it")) {
        error("Internal variable 'it' cannot be used in the REPL");
      }
    }
    
    @Override
    public REPLProgram parse() throws MyPLException {
        // <program> ::= (<tdecl> | <fdecl>)*
        REPLProgram progNode = new REPLProgram();
        advance();
        while (!match(TokenType.EOS)) {
            if (match(TokenType.TYPE))
                tdecl(progNode);
            else if (match(TokenType.FUN)) {
                inFun = true;
                fdecl(progNode);
                inFun = false;
            } else {
                stmt(progNode.stmts);
            }
        }
        advance(); // eat the EOS token
        return progNode;
    }

    @Override
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
            // if not in function, make part of expression
            CallExpr cExpr = new CallExpr();
            cExpr.funName = holdingToken;
            call_expr(cExpr);
            if (!inFun) {
              // set call expression as first term in "it" assignment expression
              AssignStmt assignStmt = pushIntoItAssignStmt(cExpr);
              stmts.add(assignStmt);
              stmts.add(printExpr());
              stmts.add(printNewLine());
            } else {
              stmts.add(cExpr);
            }
        } else {
            List<Token> lvalue = new ArrayList<>();
            lvalue.add(holdingToken);
            AssignStmt aStmt = new AssignStmt();
            aStmt.lvalue = lvalue;
            lvalue(aStmt);
            if (match(TokenType.ASSIGN) || inFun) {
                assign_stmt(aStmt);
                stmts.add(aStmt);
            } else {
                // should be "it" with value of the expression after the lvalue
                lvalue = aStmt.lvalue;
                aStmt.lvalue = new ArrayList<>();
                aStmt.lvalue.add(new Token(TokenType.ID, "it", currToken.line(), currToken.column()));
                // create rest of expression after the first IDRvalue
                IDRValue idrValue = new IDRValue();
                idrValue.path = lvalue;
                SimpleTerm sTerm = new SimpleTerm();
                sTerm.rvalue = idrValue;
                Expr expr = new Expr();
                expr.first = sTerm;
                if (isOperator()) {
                    expr.op = currToken;
                    advance();
                    Expr rest = new Expr();
                    expr(rest);
                    expr.rest = rest;
                }
                aStmt.expr = expr;
                stmts.add(aStmt);
                stmts.add(printExpr());
                stmts.add(printNewLine());
            }
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
      } else if (!inFun) {
        AssignStmt aStmt = new AssignStmt();
        aStmt.lvalue = new ArrayList<>();
        aStmt.lvalue.add(new Token(TokenType.ID, "it", currToken.line(), currToken.column()));
        aStmt.expr = new Expr();
        expr(aStmt.expr);
        stmts.add(aStmt);
        stmts.add(printExpr());
        stmts.add(printNewLine());
      } else {
        error("invalid stmt or expression");
      }
    }

    private AssignStmt pushIntoItAssignStmt(CallExpr callExpr) throws MyPLException {
      AssignStmt aStmt = new AssignStmt();
      aStmt.lvalue.add(new Token(TokenType.ID, "it", currToken.line(), currToken.column()));
      SimpleTerm sTerm = new SimpleTerm();
      sTerm.rvalue = callExpr;
      Expr expr = new Expr();
      expr.first = sTerm;
      if (isOperator()) {
          expr.op = currToken;
          advance();
          Expr rest = new Expr();
          expr(rest);
          expr.rest = rest;
      }
      aStmt.expr = expr;
      return aStmt;
    }

    private CallExpr printExpr() {
        CallExpr callExpr = new CallExpr();
        callExpr.funName = new Token(TokenType.ID, "print", currToken.line(), currToken.column());
        Expr e = new Expr();
        SimpleTerm sTerm = new SimpleTerm();
        IDRValue idrValue = new IDRValue();
        idrValue.path.add(new Token(TokenType.ID, "it", currToken.line(), currToken.column()));
        sTerm.rvalue = idrValue;
        e.first = sTerm;
        callExpr.args.add(e);
        // add newline to end of print
        return callExpr;
    }

    private CallExpr printCallExpr(CallExpr inner) {
      CallExpr callExpr = new CallExpr();
      callExpr.funName = new Token(TokenType.ID, "print", currToken.line(), currToken.column());
      Expr e = new Expr();
      SimpleTerm sTerm = new SimpleTerm();
      sTerm.rvalue = inner;
      e.first = sTerm;
      callExpr.args.add(e);
      // add newline to end of print
      return callExpr;
    }

    private CallExpr printNewLine() {
      CallExpr callExpr = new CallExpr();
      callExpr.funName = new Token(TokenType.ID, "print", currToken.line(), currToken.column());
      Expr e = new Expr();
      SimpleTerm sTerm = new SimpleTerm();
      SimpleRValue value = new SimpleRValue();
      value.value = new Token(TokenType.STRING_VAL, "\n", currToken.line(), currToken.column());
      sTerm.rvalue = value;
      e.first = sTerm;
      callExpr.args.add(e);
      // add newline to end of print
      return callExpr;
    }
}
