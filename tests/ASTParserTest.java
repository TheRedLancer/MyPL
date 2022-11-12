/*
 * File: ASTParserTest.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: Basic unit tests for the MyPL ast-based parser class.
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;


public class ASTParserTest {

  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private static ASTParser buildParser(String s) throws Exception {
    InputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
    ASTParser parser = new ASTParser(new Lexer(in));
    return parser;
  }

  private static String buildString(String... args) {
    String str = "";
    for (String s : args)
      str += s + "\n";
    return str;
  }

  //------------------------------------------------------------
  // TEST CASES
  //------------------------------------------------------------

  @Test
  public void emptyParse() throws Exception {
    ASTParser parser = buildParser("");
    Program p = parser.parse();
    assertEquals(0, p.tdecls.size());
    assertEquals(0, p.fdecls.size());
  }

  @Test
  public void oneTypeDeclInProgram() throws Exception {
    String s = buildString
      ("type Node {",
       "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.tdecls.size());
    assertEquals(0, p.fdecls.size());
  }

  @Test
  public void twoTypeDeclInProgram() throws Exception {
    String s = buildString
      ("type Node {",
       "}",
       "type Node2 {",
          "var x = (5 + func(arg1, arg2, arg3, (2 + 3 - tomato))) - \"happy\"",
          "var int j = 4",
       "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(2, p.tdecls.size());
    assertEquals(2, p.tdecls.get(1).vdecls.size());
  }
  
  @Test
  public void oneFunDeclInProgram() throws Exception {
    String s = buildString
      ("fun void main() {",
       "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(0, p.tdecls.size());
    assertEquals(1, p.fdecls.size());
  }

  @Test
  public void multipleTypeAndFunDeclsInProgram() throws Exception {
    String s = buildString
      ("type T1 {}",
       "fun void F1() {}",
       "type T2 {}",
       "fun void F2() {}",
       "fun void main() {}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(2, p.tdecls.size());
    assertEquals(3, p.fdecls.size());
  }

  @Test
  public void funDeclParam() throws Exception {
    String s = buildString
      ("fun void main(int first, char second) { }");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals("void", p.fdecls.get(0).returnType.lexeme());
    assertEquals("main", p.fdecls.get(0).funName.lexeme());
    assertEquals(2, p.fdecls.get(0).params.size());
    assertEquals("int", p.fdecls.get(0).params.get(0).paramType.lexeme());
    assertEquals("first", p.fdecls.get(0).params.get(0).paramName.lexeme());
    assertEquals(0, p.fdecls.get(0).stmts.size());
  }

  @Test
  public void condStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
        "if x > 5 {",
          "var a = 2",
        "} elif x < 5 {",
          "var b = 1",
        "} else {",
          "var c = 0",
        "}",
       "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(CondStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void forStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
        "for x from 1 upto 5 {",
          "var a = 2",
        "}",
       "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(ForStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void deleteStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
          "delete x",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(DeleteStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void returnStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
          "return x",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(ReturnStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void whileStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
          "while x < 4 {",
          "return x",
          "}",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(WhileStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void assignStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
          "x = 5",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(AssignStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void varDeclStmt() throws Exception {
    String s = buildString
      ("fun void main() {",
          "var int x = 5",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(VarDeclStmt.class, p.fdecls.get(0).stmts.get(0).getClass());
  }

  @Test
  public void callExpr() throws Exception {
    String s = buildString
      ("fun void main() {",
          "foo(x, y, 5)",
        "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    assertEquals(CallExpr.class, p.fdecls.get(0).stmts.get(0).getClass());
  }
  
}