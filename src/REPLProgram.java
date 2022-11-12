import java.util.ArrayList;
import java.util.List;

public class REPLProgram extends Program {
    public List<Stmt> stmts = new ArrayList<>();

    public void accept(StaticChecker checker) throws MyPLException {
        checker.visit(this);
    }

    public void accept(REPLCodeGenerator gen) throws MyPLException {
        gen.visit(this);
    }
}
