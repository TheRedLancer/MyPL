/*
 * File: VM.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: A bare-bones MyPL Virtual Machine. The architecture is based
 *       loosely on the architecture of the Java Virtual Machine
 *       (JVM).  Minimal error checking is done except for runtime
 *       program errors, which include: out of bound indexes,
 *       dereferencing a nil reference, and invalid value conversion
 *       (to int and double).
 */


import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Scanner;


/*----------------------------------------------------------------------

  TODO: Your main job for HW-6 is to finish the VM implementation
        below by finishing the handling of each instruction.

        Note that PUSH, NOT, JMP, READ, FREE, and NOP (trivially) are
        completed already to help get you started. 

        Be sure to look through OpCode.java to get a basic idea of
        what each instruction should do as well as the unit tests for
        additional details regarding the instructions.

        Note that you only need to perform error checking if the
        result would lead to a MyPL runtime error (where all
        compile-time errors are assumed to be found already). This
        includes things like bad indexes (in GETCHR), dereferencing
        and/or using a NIL_OBJ (see the ensureNotNil() helper
        function), and converting from strings to ints and doubles. An
        error() function is provided to help generate a MyPLException
        for such cases.

----------------------------------------------------------------------*/ 


class VM {

  // set to true to print debugging information
  private boolean DEBUG = false;
  
  // the VM's heap (free store) accessible via object-id
  private Map<Integer,Map<String,Object>> heap = new HashMap<>();
  
  // next available object-id
  private int objectId = 1111;
  
  // the frames for the program (one frame per function)
  private Map<String,VMFrame> frames = new HashMap<>();

  // the VM call stack
  private Deque<VMFrame> frameStack = new ArrayDeque<>();

  
  /**
   * For representing "nil" as a value
   */
  public static String NIL_OBJ = new String("nil");
  

  /**
   * Add a frame to the VM's list of known frames
   * @param frame the frame to add
   */
  public void add(VMFrame frame) {
    frames.put(frame.functionName(), frame);
  }

  /**
   * Turn on/off debugging, which prints out the state of the VM prior
   * to each instruction. 
   * @param debug set to true to turn on debugging (by default false)
   */
  public void setDebug(boolean debug) {
    DEBUG = debug;
  }

  public void runREPL() throws MyPLException {
    VMFrame frame = frames.get("global");
    frameStack.push(frame);

    // run loop (keep going until we run out of frames or
    // instructions) note that we assume each function returns a
    // value, and so the second check below should never occur (but is
    // useful for testing, etc).
    while (frame != null && frame.pc < frame.instructions.size()) {
      // get next instruction
      VMInstr instr = frame.instructions.get(frame.pc);
      // increment instruction pointer
      ++frame.pc;

      // For debugging: to turn on the following, call setDebug(true)
      // on the VM.
      if (DEBUG) {
        System.out.println();
        System.out.println("\t FRAME........: " + frame.functionName());
        System.out.println("\t PC...........: " + (frame.pc - 1));
        System.out.println("\t INSTRUCTION..: " + instr);
        System.out.println("\t OPERAND STACK: " + frame.operandStack);
        System.out.println("\t HEAP ........: " + heap);
      }

      
      //------------------------------------------------------------
      // Consts/Vars
      //------------------------------------------------------------

      if (instr.opcode() == OpCode.PUSH) {
        frame.operandStack.push(instr.operand());
      }

      else if (instr.opcode() == OpCode.POP) {
        frame.operandStack.pop();
      }

      else if (instr.opcode() == OpCode.LOAD) {
        int index = (int)instr.operand();
        frame.operandStack.push(frame.variables.get(index));
      }
        
      else if (instr.opcode() == OpCode.STORE) {
        Object store = frame.operandStack.pop();
        int address = (int) instr.operand();
        frame.variables.put(address, store);
      }

      
      //------------------------------------------------------------
      // Ops
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.ADD) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof String) {
          frame.operandStack.push((String)y + (String)x);
        } else if (x instanceof Integer) {
          frame.operandStack.push((int)y + (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y + (double) x);
        }
      }

      else if (instr.opcode() == OpCode.SUB) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          frame.operandStack.push((int)y - (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y - (double) x);
        }
      }

      else if (instr.opcode() == OpCode.MUL) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          frame.operandStack.push((int)y * (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y * (double) x);
        }
      }

      else if (instr.opcode() == OpCode.DIV) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          int div = (int)y / (int) x;
          frame.operandStack.push(div);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y / (double) x);
        }
      }

      else if (instr.opcode() == OpCode.MOD) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        frame.operandStack.push((int)y % (int) x);
      }

      else if (instr.opcode() == OpCode.AND) {
        Boolean x =  (boolean)frame.operandStack.pop();
        Boolean y = (boolean) frame.operandStack.pop();
        frame.operandStack.push((x && y));
      }

      else if (instr.opcode() == OpCode.OR) {
        Boolean x =  (boolean)frame.operandStack.pop();
        Boolean y = (boolean) frame.operandStack.pop();
        frame.operandStack.push((x || y));
      }

      else if (instr.opcode() == OpCode.NOT) {
        Object operand = frame.operandStack.pop();
        ensureNotNil(frame, operand);
        frame.operandStack.push(!(boolean)operand);
      }

      else if (instr.opcode() == OpCode.CMPLT) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) < 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j < (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j < (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPLE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) <= 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j <= (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j <= (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPGT) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) > 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j > (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j > (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPGE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) >= 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j >= (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j >= (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPEQ) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String && y instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.equals((String)x));
        } else if (x instanceof Integer && y instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j == (int) x);
        } else if (x instanceof Double && y instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j == (double) x);
        } else {
          frame.operandStack.push(false);
        }
      }

      else if (instr.opcode() == OpCode.CMPNE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String && y instanceof String) {
          String j = (String) y;
          frame.operandStack.push(!j.equals((String)x));
        } else if (x instanceof Integer && y instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j != (int) x);
        } else if (x instanceof Double && y instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j != (double) x);
        } else {
          frame.operandStack.push(true);
        }
      }

      else if (instr.opcode() == OpCode.NEG) {
        Object op = frame.operandStack.pop();
        if (op instanceof Integer) {
          frame.operandStack.push(-(int)op);
        } else if (op instanceof Double) {
          frame.operandStack.push(-(double)op);
        }
        
      }

      
      //------------------------------------------------------------
      // Jumps
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.JMP) {
        frame.pc = (int)instr.operand();
      }

      else if (instr.opcode() == OpCode.JMPF) {
        boolean x = (boolean) frame.operandStack.pop();
        if (!x) {
          frame.pc = (int)instr.operand();
        }
      }
        
      //------------------------------------------------------------
      // Functions
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.CALL) {
        // (1) get frame and instantiate a new copy
        String fName = (String) instr.operand();
        VMFrame newFrame = frames.get(fName).instantiate();
        
        // (2) Pop argument values off stack and push into the newFrame
        for(int i = 0; i < newFrame.argCount(); i++) {
          newFrame.operandStack.push(frame.operandStack.pop());
        }
        // (3) Push the new frame onto frame stack
        frameStack.push(newFrame);
        // (4) Set the new frame as the current frame
        frame = newFrame;
        
      }
        
      else if (instr.opcode() == OpCode.VRET) {
        // (1) pop return value off of stack
        Object ret = frame.operandStack.pop();
        // (2) remove the frame from the current frameStack
        frameStack.pop();
        // (3) set frame to the frame on the top of the stack
        frame = frameStack.peek();
        // (4) push the return value onto the operand stack of the frame
        if (frame == null) {
          return;
        }
        frame.operandStack.push(ret);
      }
        
      //------------------------------------------------------------
      // Built-ins
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.WRITE) {
        System.out.print(frame.operandStack.pop());
      }

      else if (instr.opcode() == OpCode.READ) {
        Scanner s = new Scanner(System.in);
        frame.operandStack.push(s.nextLine());
      }

      else if (instr.opcode() == OpCode.LEN) {
        String str = (String) frame.operandStack.pop();
        frame.operandStack.push(str.length());
      }

      else if (instr.opcode() == OpCode.GETCHR) {
        String str = (String) frame.operandStack.pop();
        int index = (int) frame.operandStack.pop();
        if (index >= str.length() || index < 0) {
          error("String index out of range", frame);
        }
        frame.operandStack.push(str.substring(index, index + 1));
      }

      else if (instr.opcode() == OpCode.TOINT) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof String) {
            String s = (String) obj;
            frame.operandStack.push(Integer.parseInt(s));
          } else if (obj instanceof Double) {
            double d = (double) obj;
            frame.operandStack.push((int) d);
          }
        } catch (Exception e) {
          error("Cannot cast " + obj + " to type int", frame);
        }
      }

      else if (instr.opcode() == OpCode.TODBL) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof String) {
            String s = (String) obj;
            frame.operandStack.push(Double.parseDouble(s));
          } else if (obj instanceof Integer) {
            int i = (int) obj;
            frame.operandStack.push((double) i);
          }
        } catch (Exception e) {
          error("Cannot cast " + obj + " to type double", frame);
        }
      }

      else if (instr.opcode() == OpCode.TOSTR) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof Integer) {
            Integer i = (int) obj;
            frame.operandStack.push(i.toString());
          } else if (obj instanceof Double) {
            Double d = (double) obj;
            frame.operandStack.push(d.toString());
          }
        } catch (Exception e) {
          error("Cannot cast to type String", frame);
        }
      }

      //------------------------------------------------------------
      // Heap related
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.ALLOC) {      
        List<String> fields = (List<String>) instr.operand();
        int id = objectId++;
        Map<String, Object> newMap = new HashMap<>();
        for(String field: fields) {
          newMap.put(field, null);
        }
        heap.put(id, newMap);
        frame.operandStack.push(id);
      }

      else if (instr.opcode() == OpCode.FREE) {
        // pop the oid to 
        Object oid = frame.operandStack.pop();
        ensureNotNil(frame, oid);
        // remove the object with oid from the heap
        heap.remove((int)oid);
      }

      else if (instr.opcode() == OpCode.SETFLD) {
        Object x = frame.operandStack.pop();
        int oid = (int) frame.operandStack.pop();
        String f = (String) instr.operand();
        heap.get(oid).put(f, x);
      }

      else if (instr.opcode() == OpCode.GETFLD) {  
        int oid = (int) frame.operandStack.pop();
        String f = (String) instr.operand();
        if (heap.get(oid) == null) {
          error("Cannot reference null object ID", frame);
        }
        frame.operandStack.push(heap.get(oid).get(f));
      }

      //------------------------------------------------------------
      // Special instructions
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.DUP) {
        Object x = frame.operandStack.peek();
        frame.operandStack.push(x);
      }

      else if (instr.opcode() == OpCode.SWAP) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        frame.operandStack.push(x);
        frame.operandStack.push(y);
      }

      else if (instr.opcode() == OpCode.NOP) {
        // do nothing
      }

    }
  }

  /**
   * Run the virtual machine
   */
  public void run() throws MyPLException {

    // grab the main stack frame
    if (!frames.containsKey("main"))
      throw MyPLException.VMError("No 'main' function");
    VMFrame frame = frames.get("main").instantiate();
    frameStack.push(frame);
    
    // run loop (keep going until we run out of frames or
    // instructions) note that we assume each function returns a
    // value, and so the second check below should never occur (but is
    // useful for testing, etc).
    while (frame != null && frame.pc < frame.instructions.size()) {
      // get next instruction
      VMInstr instr = frame.instructions.get(frame.pc);
      // increment instruction pointer
      ++frame.pc;

      // For debugging: to turn on the following, call setDebug(true)
      // on the VM.
      if (DEBUG) {
        System.out.println();
        System.out.println("\t FRAME........: " + frame.functionName());
        System.out.println("\t PC...........: " + (frame.pc - 1));
        System.out.println("\t INSTRUCTION..: " + instr);
        System.out.println("\t OPERAND STACK: " + frame.operandStack);
        System.out.println("\t HEAP ........: " + heap);
      }

      
      //------------------------------------------------------------
      // Consts/Vars
      //------------------------------------------------------------

      if (instr.opcode() == OpCode.PUSH) {
        frame.operandStack.push(instr.operand());
      }

      else if (instr.opcode() == OpCode.POP) {
        frame.operandStack.pop();
      }

      else if (instr.opcode() == OpCode.LOAD) {
        int index = (int)instr.operand();
        frame.operandStack.push(frame.variables.get(index));
      }
        
      else if (instr.opcode() == OpCode.STORE) {
        Object store = frame.operandStack.pop();
        int address = (int) instr.operand();
        frame.variables.put(address, store);
      }

      
      //------------------------------------------------------------
      // Ops
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.ADD) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof String) {
          frame.operandStack.push((String)y + (String)x);
        } else if (x instanceof Integer) {
          frame.operandStack.push((int)y + (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y + (double) x);
        }
      }

      else if (instr.opcode() == OpCode.SUB) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          frame.operandStack.push((int)y - (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y - (double) x);
        }
      }

      else if (instr.opcode() == OpCode.MUL) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          frame.operandStack.push((int)y * (int) x);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y * (double) x);
        }
      }

      else if (instr.opcode() == OpCode.DIV) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if (x instanceof Integer) {
          int div = (int)y / (int) x;
          frame.operandStack.push(div);
        } else if (x instanceof Double) {
          frame.operandStack.push((double)y / (double) x);
        }
      }

      else if (instr.opcode() == OpCode.MOD) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        frame.operandStack.push((int)y % (int) x);
      }

      else if (instr.opcode() == OpCode.AND) {
        Boolean x =  (boolean)frame.operandStack.pop();
        Boolean y = (boolean) frame.operandStack.pop();
        frame.operandStack.push((x && y));
      }

      else if (instr.opcode() == OpCode.OR) {
        Boolean x =  (boolean)frame.operandStack.pop();
        Boolean y = (boolean) frame.operandStack.pop();
        frame.operandStack.push((x || y));
      }

      else if (instr.opcode() == OpCode.NOT) {
        Object operand = frame.operandStack.pop();
        ensureNotNil(frame, operand);
        frame.operandStack.push(!(boolean)operand);
      }

      else if (instr.opcode() == OpCode.CMPLT) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) < 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j < (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j < (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPLE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) <= 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j <= (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j <= (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPGT) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) > 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j > (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j > (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPGE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.compareTo((String)x) >= 0);
        } else if (x instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j >= (int) x);
        } else if (x instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j >= (double) x);
        }
      }

      else if (instr.opcode() == OpCode.CMPEQ) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String && y instanceof String) {
          String j = (String) y;
          frame.operandStack.push(j.equals((String)x));
        } else if (x instanceof Integer && y instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j == (int) x);
        } else if (x instanceof Double && y instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j == (double) x);
        } else {
          frame.operandStack.push(false);
        }
      }

      else if (instr.opcode() == OpCode.CMPNE) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if (x instanceof String && y instanceof String) {
          String j = (String) y;
          frame.operandStack.push(!j.equals((String)x));
        } else if (x instanceof Integer && y instanceof Integer) {
          Integer j = (int)y;
          frame.operandStack.push(j != (int) x);
        } else if (x instanceof Double && y instanceof Double) {
          double j = (double)y;
          frame.operandStack.push(j != (double) x);
        } else {
          frame.operandStack.push(true);
        }
      }

      else if (instr.opcode() == OpCode.NEG) {
        Object op = frame.operandStack.pop();
        if (op instanceof Integer) {
          frame.operandStack.push(-(int)op);
        } else if (op instanceof Double) {
          frame.operandStack.push(-(double)op);
        }
        
      }

      
      //------------------------------------------------------------
      // Jumps
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.JMP) {
        frame.pc = (int)instr.operand();
      }

      else if (instr.opcode() == OpCode.JMPF) {
        boolean x = (boolean) frame.operandStack.pop();
        if (!x) {
          frame.pc = (int)instr.operand();
        }
      }
        
      //------------------------------------------------------------
      // Functions
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.CALL) {
        // (1) get frame and instantiate a new copy
        String fName = (String) instr.operand();
        VMFrame newFrame = frames.get(fName).instantiate();
        
        // (2) Pop argument values off stack and push into the newFrame
        for(int i = 0; i < newFrame.argCount(); i++) {
          newFrame.operandStack.push(frame.operandStack.pop());
        }
        // (3) Push the new frame onto frame stack
        frameStack.push(newFrame);
        // (4) Set the new frame as the current frame
        frame = newFrame;
        
      }
        
      else if (instr.opcode() == OpCode.VRET) {
        // (1) pop return value off of stack
        Object ret = frame.operandStack.pop();
        // (2) remove the frame from the current frameStack
        frameStack.pop();
        // (3) set frame to the frame on the top of the stack
        frame = frameStack.peek();
        // (4) push the return value onto the operand stack of the frame
        if (frame == null) {
          return;
        }
        frame.operandStack.push(ret);
      }
        
      //------------------------------------------------------------
      // Built-ins
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.WRITE) {
        System.out.print(frame.operandStack.pop());
      }

      else if (instr.opcode() == OpCode.READ) {
        Scanner s = new Scanner(System.in);
        frame.operandStack.push(s.nextLine());
      }

      else if (instr.opcode() == OpCode.LEN) {
        String str = (String) frame.operandStack.pop();
        frame.operandStack.push(str.length());
      }

      else if (instr.opcode() == OpCode.GETCHR) {
        String str = (String) frame.operandStack.pop();
        int index = (int) frame.operandStack.pop();
        if (index >= str.length() || index < 0) {
          error("String index out of range", frame);
        }
        frame.operandStack.push(str.substring(index, index + 1));
      }

      else if (instr.opcode() == OpCode.TOINT) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof String) {
            String s = (String) obj;
            frame.operandStack.push(Integer.parseInt(s));
          } else if (obj instanceof Double) {
            double d = (double) obj;
            frame.operandStack.push((int) d);
          }
        } catch (Exception e) {
          error("Cannot cast " + obj + " to type int", frame);
        }
      }

      else if (instr.opcode() == OpCode.TODBL) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof String) {
            String s = (String) obj;
            frame.operandStack.push(Double.parseDouble(s));
          } else if (obj instanceof Integer) {
            int i = (int) obj;
            frame.operandStack.push((double) i);
          }
        } catch (Exception e) {
          error("Cannot cast " + obj + " to type double", frame);
        }
      }

      else if (instr.opcode() == OpCode.TOSTR) {
        Object obj =  frame.operandStack.pop();
        try {
          if (obj instanceof Integer) {
            Integer i = (int) obj;
            frame.operandStack.push(i.toString());
          } else if (obj instanceof Double) {
            Double d = (double) obj;
            frame.operandStack.push(d.toString());
          }
        } catch (Exception e) {
          error("Cannot cast to type String", frame);
        }
      }

      //------------------------------------------------------------
      // Heap related
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.ALLOC) {      
        List<String> fields = (List<String>) instr.operand();
        int id = objectId++;
        Map<String, Object> newMap = new HashMap<>();
        for(String field: fields) {
          newMap.put(field, null);
        }
        heap.put(id, newMap);
        frame.operandStack.push(id);
      }

      else if (instr.opcode() == OpCode.FREE) {
        // pop the oid to 
        Object oid = frame.operandStack.pop();
        ensureNotNil(frame, oid);
        // remove the object with oid from the heap
        heap.remove((int)oid);
      }

      else if (instr.opcode() == OpCode.SETFLD) {
        Object x = frame.operandStack.pop();
        int oid = (int) frame.operandStack.pop();
        String f = (String) instr.operand();
        heap.get(oid).put(f, x);
      }

      else if (instr.opcode() == OpCode.GETFLD) {  
        int oid = (int) frame.operandStack.pop();
        String f = (String) instr.operand();
        if (heap.get(oid) == null) {
          error("Cannot reference null object ID", frame);
        }
        frame.operandStack.push(heap.get(oid).get(f));
      }

      //------------------------------------------------------------
      // Special instructions
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.DUP) {
        Object x = frame.operandStack.peek();
        frame.operandStack.push(x);
      }

      else if (instr.opcode() == OpCode.SWAP) {
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        frame.operandStack.push(x);
        frame.operandStack.push(y);
      }

      else if (instr.opcode() == OpCode.NOP) {
        // do nothing
      }

    }
  }

  
  // to print the lists of instructions for each VM Frame
  @Override
  public String toString() {
    String s = "";
    for (Map.Entry<String,VMFrame> e : frames.entrySet()) {
      String funName = e.getKey();
      s += "Frame '" + funName + "'\n";
      List<VMInstr> instructions = e.getValue().instructions;      
      for (int i = 0; i < instructions.size(); ++i) {
        VMInstr instr = instructions.get(i);
        s += "  " + i + ": " + instr + "\n";
      }
      // s += "\n";
    }
    return s;
  }

  
  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------

  // error
  private void error(String m, VMFrame f) throws MyPLException {
    int pc = f.pc - 1;
    VMInstr i = f.instructions.get(pc);
    String name = f.functionName();
    m += " (in " + name + " at " + pc + ": " + i + ")";
    throw MyPLException.VMError(m);
  }

  // error if given value is nil
  private void ensureNotNil(VMFrame f, Object v) throws MyPLException {
    if (v == NIL_OBJ)
      error("Nil reference", f);
  }
  
  
}
