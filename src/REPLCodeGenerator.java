import java.util.HashMap;
import java.util.Map;

public class REPLCodeGenerator extends CodeGenerator {

    protected VMFrame globalFrame;
    protected Map<String,Integer> globalVarMap;
    protected int globalVarIndex;
    
    public REPLCodeGenerator(TypeInfo typeInfo, VM vm, VMFrame global) {
        super(typeInfo, vm);
        this.globalFrame = global;
        globalVarMap = new HashMap<String,Integer>();
        globalVarIndex = 0;
        globalVarMap.put("it", globalVarIndex);
        ++globalVarIndex;
    }
    
    public void visit(REPLProgram node) throws MyPLException {
        // store UDTs for later
        for (TypeDecl tdecl : node.tdecls) {
            // add a mapping from type name to the TypeDecl
            typeDecls.put(tdecl.typeName.lexeme(), tdecl);
        }
        // only need to translate the function declarations
        for (FunDecl fdecl : node.fdecls)
            fdecl.accept(this);
        
        // We want to add these instructions to the global VMFrame
        // Initialize the CodeGenerator with global values
        currFrame = globalFrame;
        varMap = globalVarMap;
        currVarIndex = globalVarIndex;
        for (Stmt stmt : node.stmts) {
            stmt.accept(this);
        }
        // store global values for next instructions
        globalFrame = currFrame;
        globalVarMap = varMap;
        globalVarIndex = currVarIndex;
    }

    public VMFrame getGlobalFrame() {
        return globalFrame;
    }
}
