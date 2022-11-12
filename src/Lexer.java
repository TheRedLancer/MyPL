/*
 * File: Lexer.java
 * Date: Spring 2022
 * Auth: Zach Burnaby
 * Desc: This tokenizes a MyPL file
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;


public class Lexer {

  private BufferedReader buffer; // handle to input stream
  private int line = 1;          // current line number
  private int column = 0;        // current column number


  //--------------------------------------------------------------------
  // Constructor
  //--------------------------------------------------------------------
  
  public Lexer(InputStream instream) {
    buffer = new BufferedReader(new InputStreamReader(instream));
  }


  //--------------------------------------------------------------------
  // Private helper methods
  //--------------------------------------------------------------------

  // Returns next character in the stream. Returns -1 if end of file.
  private int read() throws MyPLException {
    try {
      return buffer.read();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return -1;
  }

  
  // Returns next character without removing it from the stream.
  private int peek() throws MyPLException {
    int ch = -1;
    try {
      buffer.mark(1);
      ch = read();
      buffer.reset();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return ch;
  }


  // Print an error message and exit the program.
  private void error(String msg, int line, int column) throws MyPLException {
    msg = msg + " at line " + line + ", column " + column;
    throw MyPLException.LexerError(msg);
  }

  
  // Checks for whitespace 
  public static boolean isWhitespace(int ch) {
    return Character.isWhitespace((char)ch);
  }

  
  // Checks for digit
  private static boolean isDigit(int ch) {
    return Character.isDigit((char)ch);
  }

  
  // Checks for letter
  private static boolean isLetter(int ch) {
    return Character.isLetter((char)ch);
  }

  
  // Checks if given symbol
  private static boolean isSymbol(int ch, char symbol) {
    return (char)ch == symbol;
  }

  
  // Checks if end-of-file
  private static boolean isEOF(int ch) {
    return ch == -1;
  }
  

  //--------------------------------------------------------------------
  // Public next_token function
  //--------------------------------------------------------------------
  
  // Returns next token in input stream
  public Token nextToken() throws MyPLException {
    // TODO: implement nextToken()

    // Note: Use the error() function to report errors, e.g.:
    //         error("error msg goes here", line, column);

    // Reminder: Comment your code and fill in the file header comment
    // above
    
    // The following returns the end-of-file token, you'll need to
    // remove this to implement nextToken (it is only here so that the
    // program can compile initially)
    // return new Token(TokenType.EOS, "end-of-file", line, column);
    String lexeme = "";
    Token token = null;
    int columnsToAdd = 0;

    while (isWhitespace(peek()) || isSymbol(peek(), '#')) {
      if (isSymbol(peek(), '#')) {
        // Read all chars in a comment line until newline char
        while ((char)peek() != '\n') {
          read();
        }
      }
      if (isSymbol(peek(), ' ')) {
        column += 1;
      } else if (isSymbol(peek(), '\n')) {
        column = 0;
        line += 1;
      }
      read();
    }
    if (isEOF(peek())) {
      lexeme += "end-of-file";
      column += 1;
      return new Token(TokenType.EOS, lexeme, line, column);
    }
    // no more comments, whitespace, or EOF, must be start of token
    lexeme += (char)read();
    column += 1;
    // check all single char tokens
    if (lexeme.charAt(0) == ',') {
      token = new Token(TokenType.COMMA, lexeme, line, column);

    } else if (lexeme.charAt(0) == '.') {
      token = new Token(TokenType.DOT, lexeme, line, column);

    } else if (lexeme.charAt(0) == '+') {
      token = new Token(TokenType.PLUS, lexeme, line, column);

    } else if (lexeme.charAt(0) == '-') {
      token = new Token(TokenType.MINUS, lexeme, line, column);

    } else if (lexeme.charAt(0) == '*') {
      token = new Token(TokenType.MULTIPLY, lexeme, line, column);

    } else if (lexeme.charAt(0) == '/') {
      token = new Token(TokenType.DIVIDE, lexeme, line, column);

    } else if (lexeme.charAt(0) == '%') {
      token = new Token(TokenType.MODULO, lexeme, line, column);

    } else if (lexeme.charAt(0) == '{') {
      token = new Token(TokenType.LBRACE, lexeme, line, column);

    } else if (lexeme.charAt(0) == '}') {
      token = new Token(TokenType.RBRACE, lexeme, line, column);

    } else if (lexeme.charAt(0) == '(') {
      token = new Token(TokenType.LPAREN, lexeme, line, column);

    } else if (lexeme.charAt(0) == ')') {
      token = new Token(TokenType.RPAREN, lexeme, line, column);

    } else if (lexeme.charAt(0) == '=') {
      if (isSymbol(peek(), '=')) {
        lexeme += (char)read();
        token = new Token(TokenType.EQUAL, lexeme, line, column);
        column += 1;
      } else {
        token = new Token(TokenType.ASSIGN, lexeme, line, column);
      }
    } else if (lexeme.charAt(0) == '>') {
      if (isSymbol(peek(), '=')) {
        lexeme += (char)read();
        token = new Token(TokenType.GREATER_THAN_EQUAL, lexeme, line, column);
        column += 1;
      } else {
        token = new Token(TokenType.GREATER_THAN, lexeme, line, column);
      }
    } else if (lexeme.charAt(0) == '<') {
      if (isSymbol(peek(), '=')) {
        lexeme += (char)read();
        token = new Token(TokenType.LESS_THAN_EQUAL, lexeme, line, column);
        column += 1;
      } else {
        token = new Token(TokenType.LESS_THAN, lexeme, line, column);
      }
    } else if (lexeme.charAt(0) == '!') {
      if (isSymbol(peek(), '=')) {
        lexeme += (char)read();
        token = new Token(TokenType.NOT_EQUAL, lexeme, line, column);
        column += 1;
      } else {
        column += 1;
        error("expecting '=', found '" + (char)peek() + "'", line, column);
      }
    }
    // Next are char literals
    else if (lexeme.charAt(0) == '\'') {
      if (isEOF(peek())) {
        error("empty character" , line, column);
      }
      lexeme = Character.toString((char)read());
      columnsToAdd += 1;
      if (lexeme.charAt(0) == '\n') {
        column += 1;
        error("found newline in character", line, column);
      }
      if (lexeme.charAt(0) == '\'') {
        error("empty character" , line, column);
      }
      if (lexeme.charAt(0) == '\\' && !isSymbol(peek(), '\'')) {
        // If the first symbol is a backslash, and next symbol is not the closing single-quote
        // means backslash is escape character and we need to read the next char
        lexeme += (char)read();
        columnsToAdd += 1;
      }
      // handle closing single-quote
      if (isSymbol(peek(), '\'')) {
        read();
        columnsToAdd += 1;
      } else {
        column += 1;
        error("expecting ' found, '" + (char)peek() + "'" , line, column);
      }
      token = new Token(TokenType.CHAR_VAL, lexeme, line, column);
      column += columnsToAdd;

    } else if (lexeme.charAt(0) == '\"' && isSymbol(peek(), '\"')) {
      read();
      token = new Token(TokenType.STRING_VAL, "", line, column);
      column += 1;
    } else if (lexeme.charAt(0) == '\"') {
      // String next, read until closing double-quote or new line
      // If we reach a new line, throw error
      if (isSymbol(peek(), '\n') || isSymbol(peek(), '\r')) {
        column += 1;
        error("found newline within string", line, column);
      }
      lexeme = Character.toString((char)read());
      columnsToAdd += 1;
      while (!isSymbol(peek(), '\"')) {
        if (isEOF(peek())) {
          column += columnsToAdd;
          error("found end-of-file in string", line, column);
        }
        if (isSymbol(peek(), '\n') || isSymbol(peek(), '\r')) {
          error("found newline within string", line, column);
        } else {
          lexeme += (char)read();
          columnsToAdd += 1;
        }
      }
      token = new Token(TokenType.STRING_VAL, lexeme, line, column);
      read(); // read the last double-quote
      columnsToAdd += 1;
      column += columnsToAdd;

    } else if (Character.isDigit(lexeme.charAt(0))) {
      // INT or DOUBLE
      while (isDigit(peek())) {
        lexeme += (char)read();
        columnsToAdd += 1;
      }
      // Reached the end of the integer digit
      if (isSymbol(peek(), '.')) {
        // Next char is a period means there is a double
        lexeme += (char)read();
        columnsToAdd += 1;

        while (isDigit(peek())) {
          lexeme += (char)read();
          columnsToAdd += 1;
        }
        token = new Token(TokenType.DOUBLE_VAL, lexeme, line, column);
      } else if (!isLetter(peek())) {
        // Whitespace or other valid char is after Integer
        token = new Token(TokenType.INT_VAL, lexeme, line, column);
      } 
      if (isLetter(peek())) {
        error("missing decimal digit in double value '" + lexeme + "'", line, column);
      }
      if (isSymbol(peek(), '.')) {
        error("too many decimal points in double value '" + lexeme + "'", line, column);
      }
      if (lexeme.charAt(0) == '0' && lexeme.length() > 1 && lexeme.charAt(1) != '.') {
        error("leading zero in '" + lexeme + "'", line, column);
      } 
      column += columnsToAdd;

    } else if (lexeme.charAt(0) == '_') {
      error("ID cannot begin with underscore", line, column);
    } else {
      if (!Character.isLetter(lexeme.charAt(0)) && lexeme.charAt(0) != '_') {
        error("invalid symbol '" + lexeme + "'", line, column);
      }
      // read in the whole word
      while (isLetter(peek()) || isSymbol(peek(), '_') || isDigit(peek())) {
        lexeme += (char)read();
        columnsToAdd += 1;
      }
      // compare to reserved words
      switch (lexeme) {
        case "and":
          token = new Token(TokenType.AND, lexeme, line, column);
          break;
        case "or":
          token = new Token(TokenType.OR, lexeme, line, column);
          break;
        case "not":
          token = new Token(TokenType.NOT, lexeme, line, column);
          break;
        case "int":
          token = new Token(TokenType.INT_TYPE, lexeme, line, column);
          break;
        case "double":
          token = new Token(TokenType.DOUBLE_TYPE, lexeme, line, column);
          break;
        case "bool":
          token = new Token(TokenType.BOOL_TYPE, lexeme, line, column);
          break;
        case "char":
          token = new Token(TokenType.CHAR_TYPE, lexeme, line, column);
          break;
        case "string":
          token = new Token(TokenType.STRING_TYPE, lexeme, line, column);
          break;
        case "void":
          token = new Token(TokenType.VOID_TYPE, lexeme, line, column);
          break;
        case "nil":
          token = new Token(TokenType.NIL, lexeme, line, column);
          break;
        case "var":
          token = new Token(TokenType.VAR, lexeme, line, column);
          break;
        case "true":
          token = new Token(TokenType.BOOL_VAL, lexeme, line, column);
          break;
        case "false":
          token = new Token(TokenType.BOOL_VAL, lexeme, line, column);
          break;
        case "type":
          token = new Token(TokenType.TYPE, lexeme, line, column);
          break;
        case "while":
          token = new Token(TokenType.WHILE, lexeme, line, column);
          break;
        case "for":
          token = new Token(TokenType.FOR, lexeme, line, column);
          break;
        case "from":
          token = new Token(TokenType.FROM, lexeme, line, column);
          break;
        case "upto":
          token = new Token(TokenType.UPTO, lexeme, line, column);
          break;
        case "downto":
          token = new Token(TokenType.DOWNTO, lexeme, line, column);
          break;
        case "if":
          token = new Token(TokenType.IF, lexeme, line, column);
          break;
        case "elif":
          token = new Token(TokenType.ELIF, lexeme, line, column);
          break;
        case "else":
          token = new Token(TokenType.ELSE, lexeme, line, column);
          break;
        case "fun":
          token = new Token(TokenType.FUN, lexeme, line, column);
          break;
        case "new":
          token = new Token(TokenType.NEW, lexeme, line, column);
          break;
        case "delete":
          token = new Token(TokenType.DELETE, lexeme, line, column);
          break;
        case "return":
          token = new Token(TokenType.RETURN, lexeme, line, column);
          break;
        case "neg":
          token = new Token(TokenType.NEG, lexeme, line, column);
          break;
        default:
          token = new Token(TokenType.ID, lexeme, line, column);
      }
      column += columnsToAdd;
    }

    if (token == null) {
      token = new Token(TokenType.EOS, "end-of-file", line, column);
    }
    return token;
  }

}
