import java.io.PrintStream;

public class REPLPrintVisitor extends PrintVisitor {

    public REPLPrintVisitor(PrintStream printStream) {
        super(printStream);
    }

    public void visit(REPLProgram node) throws MyPLException {
        // print type decls first
        for (TypeDecl d : node.tdecls)
            d.accept(this);
        // print function decls second
        for (FunDecl d : node.fdecls)
            d.accept(this);
        for (Stmt t : node.stmts) {
            t.accept(this);
            out.println();
        }
    }
    
}
