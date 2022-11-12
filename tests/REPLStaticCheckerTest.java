/*
 * File: REPLStatiCheckerTest.java
 * Date: Spring 2022
 * Auth: Z. Burnaby
 * Desc: REPL extension of StaticChecker tests
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;


public class REPLStaticCheckerTest {

  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------

  private static ASTParser buildParser(String s) throws Exception {
    InputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
    REPLASTParser parser = new REPLASTParser(new Lexer(in));
    return parser;
  }
  
  private static StaticChecker buildChecker() throws Exception {
    TypeInfo types = new TypeInfo();
    StaticChecker checker = new StaticChecker(types);
    return checker;
  }

  private static String buildString(String... args) {
    String str = "";
    for (String s : args)
      str += s + "\n";
    return str;
  }

  @Test
  public void simpleProgramCheck() throws Exception {
    String s = buildString("12 + 4");
    buildParser(s).parse().accept(buildChecker());
  }

}