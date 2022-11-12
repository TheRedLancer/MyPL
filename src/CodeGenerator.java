/*
 * File: CodeGenerator.java
 * Date: Spring 2022
 * Auth:
 * Desc: 
 */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class CodeGenerator implements Visitor {

  // the user-defined type and function type information
  protected TypeInfo typeInfo = null;

  // the virtual machine to add the code to
  protected VM vm = null;

  // the current frame
  protected VMFrame currFrame = null;

  // mapping from variables to their indices (in the frame)
  protected Map<String,Integer> varMap = null;

  // the current variable index (in the frame)
  protected int currVarIndex = 0;

  // to keep track of the typedecl objects for initialization
  protected Map<String,TypeDecl> typeDecls = new HashMap<>();


  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------
  
  // helper function to clean up uneeded NOP instructions
  private void fixNoOp() {
    int nextIndex = currFrame.instructions.size();
    // check if there are any instructions
    if (nextIndex == 0)
      return;
    // get the last instuction added
    VMInstr instr = currFrame.instructions.get(nextIndex - 1);
    // check if it is a NOP
    if (instr.opcode() == OpCode.NOP)
      currFrame.instructions.remove(nextIndex - 1);
  }

  private void fixCallStmt(Stmt s) {
    // get the last instuction added
    if (s instanceof CallExpr) {
      VMInstr instr = VMInstr.POP();
      instr.addComment("clean up call return value");
      currFrame.instructions.add(instr);
    }

  }
  
  //----------------------------------------------------------------------  
  // Constructor
  //----------------------------------------------------------------------

  public CodeGenerator(TypeInfo typeInfo, VM vm) {
    this.typeInfo = typeInfo;
    this.vm = vm;
  }

  //----------------------------------------------------------------------
  // VISITOR FUNCTIONS
  //----------------------------------------------------------------------
  
  public void visit(Program node) throws MyPLException {
    currVarIndex = 0;
    // store UDTs for later
    for (TypeDecl tdecl : node.tdecls) {
      // add a mapping from type name to the TypeDecl
      typeDecls.put(tdecl.typeName.lexeme(), tdecl);
    }
    // only need to translate the function declarations
    for (FunDecl fdecl : node.fdecls)
      fdecl.accept(this);
  }

  public void visit(TypeDecl node) throws MyPLException {
    // Intentionally left blank -- nothing to do here
  }
  
  public void visit(FunDecl node) throws MyPLException {
    currVarIndex = 0;
    // 1. create a new frame for the function
    currFrame = new VMFrame(node.funName.lexeme(), node.params.size());
    vm.add(currFrame);
    // 2. create a variable mapping for the frame
    varMap = new HashMap<String,Integer>();
    // 3. store args
    for (int i = 0; i < node.params.size(); i++) {
      varMap.put(node.params.get(i).paramName.lexeme(), i);
      currFrame.instructions.add(VMInstr.STORE(currVarIndex));
      ++currVarIndex;
    }
    // 4. visit statement nodes
    boolean retStmtFlag = false;
    for (Stmt stmt: node.stmts) {
      stmt.accept(this);
      retStmtFlag = stmt instanceof ReturnStmt;
      fixCallStmt(stmt);
    }
    // 5. check to see if the last statement was a return (if not, add
    //    return nil)
    if (!retStmtFlag) {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
      currFrame.instructions.add(VMInstr.VRET());
    }
    currVarIndex = 0;
  }
  
  public void visit(VarDeclStmt node) throws MyPLException {
    node.expr.accept(this);
    varMap.put(node.varName.lexeme(), currVarIndex);
    currFrame.instructions.add(VMInstr.STORE(currVarIndex));
    ++currVarIndex;
  }
  
  public void visit(AssignStmt node) throws MyPLException {
    node.expr.accept(this);
    if (node.lvalue.size() > 1) {
      currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.lvalue.get(0).lexeme())));
      //GETFIELD if there are more than 2 in the path, starting with the second one
      for (int i = 1; i < node.lvalue.size() - 1; i++) {
        currFrame.instructions.add(VMInstr.GETFLD(node.lvalue.get(i).lexeme()));
      }
      //swap and setfield
      currFrame.instructions.add(VMInstr.SWAP());
      currFrame.instructions.add(VMInstr.SETFLD(node.lvalue.get(node.lvalue.size() - 1).lexeme()));
    } else {
      currFrame.instructions.add(VMInstr.STORE(varMap.get(node.lvalue.get(0).lexeme())));
    }

  }
  
  public void visit(CondStmt node) throws MyPLException {
    node.ifPart.cond.accept(this);
    // if (!node.cond) jump to else or end
    currFrame.instructions.add(VMInstr.JMPF(-1)); // jump to next elif/else
    int jump = currFrame.instructions.size() - 1;
    for (Stmt stmt: node.ifPart.stmts) {
      stmt.accept(this);
    }
    currFrame.instructions.add(VMInstr.JMP(-1));

    // add the end jump line # to a list
    List<Integer> endJumps = new ArrayList<>();
    endJumps.add(currFrame.instructions.size() - 1);

    if (node.elifs != null) {
      // Work through each Elif
      for (BasicIf bIf : node.elifs) {
        // update previous conditional jump to be the next instruction
        currFrame.instructions.get(jump).updateOperand(currFrame.instructions.size());
        currFrame.instructions.add(VMInstr.NOP()); // nop in case elif is empty
        bIf.cond.accept(this);
        currFrame.instructions.add(VMInstr.JMPF(-1)); // jump to next elif/else
        jump = currFrame.instructions.size() - 1;
        for (Stmt stmt: bIf.stmts) {
          stmt.accept(this);
        }
        currFrame.instructions.add(VMInstr.JMP(-1));
        // add the end jump line # to a list
        endJumps.add(currFrame.instructions.size() - 1);
      }
    }

    currFrame.instructions.get(jump).updateOperand(currFrame.instructions.size());
    currFrame.instructions.add(VMInstr.NOP());
    if (node.elseStmts != null) {
      for (Stmt stmt: node.elseStmts) {
        stmt.accept(this);
      }
    }
    // set all of the endJumps to next instruction
    for (int j: endJumps) {
      currFrame.instructions.get(j).updateOperand(currFrame.instructions.size());
    }
    currFrame.instructions.add(VMInstr.NOP());
    

  }

  public void visit(WhileStmt node) throws MyPLException {
    int startInstr = currFrame.instructions.size();
    node.cond.accept(this);
    currFrame.instructions.add(VMInstr.JMPF(-1));
    int jumpF = currFrame.instructions.size() - 1;
    for (Stmt stmt: node.stmts) {
      stmt.accept(this);
      fixCallStmt(stmt);
    }
    currFrame.instructions.add(VMInstr.JMP(startInstr));
    currFrame.instructions.add(VMInstr.NOP());
    currFrame.instructions.get(jumpF).updateOperand(currFrame.instructions.size() - 1);
  }

  public void visit(ForStmt node) throws MyPLException {
    // init for var
    varMap.put(node.varName.lexeme(), currVarIndex);
    ++currVarIndex;
    // set for var to initial value
    node.start.accept(this);
    currFrame.instructions.add(VMInstr.STORE(varMap.get(node.varName.lexeme())));
    // compare forVar with end expr
    int startInstr = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    node.end.accept(this);
    if (node.upto) {
      currFrame.instructions.add(VMInstr.CMPLE());
    } else {
      currFrame.instructions.add(VMInstr.CMPGE());
    }
    currFrame.instructions.add(VMInstr.JMPF(-1));
    int jumpF = currFrame.instructions.size() - 1;
    for (Stmt stmt : node.stmts) {
      stmt.accept(this);
      fixCallStmt(stmt);
    }
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.PUSH(1));
    if (node.upto) {
      currFrame.instructions.add(VMInstr.ADD());
    } else {
      currFrame.instructions.add(VMInstr.SUB());
    }
    currFrame.instructions.add(VMInstr.STORE(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.JMP(startInstr));
    currFrame.instructions.add(VMInstr.NOP());
    currFrame.instructions.get(jumpF).updateOperand(currFrame.instructions.size() - 1);
  }
  
  public void visit(ReturnStmt node) throws MyPLException {
    if (node.expr == null) {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    } else {
      node.expr.accept(this);
    }
    currFrame.instructions.add(VMInstr.VRET());
  }
  
  
  public void visit(DeleteStmt node) throws MyPLException {
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.FREE());
  }

  public void visit(CallExpr node) throws MyPLException {
    // push args (in order)
    for (Expr arg : node.args)
      arg.accept(this);
    // built-in functions:
    if (node.funName.lexeme().equals("print")) {
      currFrame.instructions.add(VMInstr.WRITE());
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
    else if (node.funName.lexeme().equals("read"))
      currFrame.instructions.add(VMInstr.READ());
    else if (node.funName.lexeme().equals("length"))
      currFrame.instructions.add(VMInstr.LEN());
    else if (node.funName.lexeme().equals("get"))
      currFrame.instructions.add(VMInstr.GETCHR());
    else if (node.funName.lexeme().equals("stoi") || node.funName.lexeme().equals("dtoi"))
      currFrame.instructions.add(VMInstr.TOINT());
    else if (node.funName.lexeme().equals("stod") || node.funName.lexeme().equals("itod"))
      currFrame.instructions.add(VMInstr.TODBL());
    else if (node.funName.lexeme().equals("dtos") || node.funName.lexeme().equals("itos"))
      currFrame.instructions.add(VMInstr.TOSTR());
    // user-defined functions
    else
      currFrame.instructions.add(VMInstr.CALL(node.funName.lexeme()));
  }
  
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.value.type() == TokenType.INT_VAL) {
      int val = Integer.parseInt(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.DOUBLE_VAL) {
      double val = Double.parseDouble(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.BOOL_VAL) {
      if (node.value.lexeme().equals("true"))
        currFrame.instructions.add(VMInstr.PUSH(true));
      else
        currFrame.instructions.add(VMInstr.PUSH(false));        
    }
    else if (node.value.type() == TokenType.CHAR_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.STRING_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.NIL) {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
  }
  
  public void visit(NewRValue node) throws MyPLException {
    List<String> memberVars = new ArrayList<>(typeInfo.components(node.typeName.lexeme()));
    currFrame.instructions.add(VMInstr.ALLOC(memberVars));
    for (VarDeclStmt var: typeDecls.get(node.typeName.lexeme()).vdecls) {
      currFrame.instructions.add(VMInstr.DUP());
      var.expr.accept(this);
      currFrame.instructions.add(VMInstr.SETFLD(var.varName.lexeme()));
    }
  }
  
  public void visit(IDRValue node) throws MyPLException {
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.path.get(0).lexeme())));
    if (node.path.size() > 1) {
      for (int i = 1; i < node.path.size(); i++) {
        currFrame.instructions.add(VMInstr.GETFLD(node.path.get(i).lexeme()));
      }
    }
  }
      
  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
    currFrame.instructions.add(VMInstr.NEG());
  }

  public void visit(Expr node) throws MyPLException {
    node.first.accept(this);
    if (node.op != null) {
      node.rest.accept(this);
      switch (node.op.lexeme()) {
        case "+":
          currFrame.instructions.add(VMInstr.ADD());
          break;
        case "-":
          currFrame.instructions.add(VMInstr.SUB());
          break;
        case "/":
          currFrame.instructions.add(VMInstr.DIV());
          break;
        case "*":
          currFrame.instructions.add(VMInstr.MUL());
          break;
        case "%":
          currFrame.instructions.add(VMInstr.MOD());
          break;
        case "and":
          currFrame.instructions.add(VMInstr.AND());
          break;
        case "or":
          currFrame.instructions.add(VMInstr.OR());
          break;
        case "==":
          currFrame.instructions.add(VMInstr.CMPEQ());
          break;
        case "!=":
          currFrame.instructions.add(VMInstr.CMPNE());
          break;
        case ">":
          currFrame.instructions.add(VMInstr.CMPGT());
          break;
        case ">=":
          currFrame.instructions.add(VMInstr.CMPGE());
          break;
        case "<":
          currFrame.instructions.add(VMInstr.CMPLT());
          break;
        case "<=":
          currFrame.instructions.add(VMInstr.CMPLE());
          break;
        
      }
    }
    if (node.logicallyNegated) {
      currFrame.instructions.add(VMInstr.NOT());
    }

  }

  public void visit(SimpleTerm node) throws MyPLException {
    // defer to contained rvalue
    node.rvalue.accept(this);
  }
  
  public void visit(ComplexTerm node) throws MyPLException {
    // defer to contained expression
    node.expr.accept(this);
  }

}
