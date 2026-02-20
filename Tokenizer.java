// in/out
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// data structures
import java.util.List;
import java.util.ArrayList;

/* Token Numbers
 *
 * RESERVED WORDS: [1-11]
 * SPECIAL SYMBOLS: [12-30]
 * INTEGERS: 31
 * INDENTIFIERS: 32
 * EOF: 33
 * ERROR_TOKEN: 34
*/


public class Tokenizer {

  // token struct to hold both token # and the token itself
  private static class Token {
    int t; // type 
    String s; // token string
    Token(int t, String s) {
      this.t = t;
      this.s = s;
    }
  }

  // data & 'state'  
  private BufferedReader buff;
  private List<Token> currentLine = new ArrayList<>();
  private int cursor = 0;
  //private boolean eof = false; 

  public Tokenizer(String file) {
    try {
      buff = new BufferedReader(new FileReader(file));
      tokenizeLine();
    } catch (IOException err) {
      System.err.println("ERROR: Could not open file: " + file);
      System.exit(1); // process::exit(1) 
    }
  }
  
  private void tokenizeLine() {

    currentLine.clear(); // empty & reset cursor for new line
    cursor = 0;

    try {
      String line = buff.readLine();

      // ensure we have content
      while(line!=null && line.trim().isEmpty()) {
        line = buff.readLine();
      }
      // if we have passed previous while statement & line is null then it has to be eof
      if(line == null) {
        //eof = true;
        currentLine.add(new Token(33, "EOF"));
        return;
      }

      int index = 0;
      while(index < line.length()) {
        char c = line.charAt(index);
        
        // ignore initial whitespace
        if(Character.isWhitespace(c)) {
          index++;
          continue;
        }

        //IDENTIFIERS
        // "start with uppercase letter, followed by zero
        // or more uppercase letters and zero or more digits"
        if(Character.isUpperCase(c)) {
          StringBuilder str = new StringBuilder();
          
          // '[A-Z] + ('[A-Z]*' || '[0-9]*')
          while(index < line.length() && (Character.isUpperCase(line.charAt(index)) || Character.isDigit(line.charAt(index)))) {
            str.append(line.charAt(index));
            index++;
          }
          
          // eliminates the 'ABCend' illegal error (hopefully)
          if(index < line.length() && Character.isLowerCase(line.charAt(index))) {
            currentLine.add(new Token(34, "ERROR_TOKEN"));
            return;
          }
          currentLine.add(new Token(32, str.toString()));
          continue;
        }

        //INTEGERS
        // unsigned
        if(Character.isDigit(c)) {
          StringBuilder str = new StringBuilder();

          // '[0-9]*'
          while(index < line.length() && Character.isDigit(line.charAt(index))) {
            str.append(line.charAt(index));
            index++;
          }
          currentLine.add(new Token(31, str.toString()));
          continue;
        }

        //RESERVED WORDS
        // has to start with a lowercase letter.
        if(Character.isLowerCase(c)) {
          StringBuilder str = new StringBuilder();
          while(index < line.length() && Character.isLowerCase(line.charAt(index))) {
            str.append(line.charAt(index));
            index++;
          }

          int t = getResType(str.toString()); // will return [1-11] unless error, then itll return -1 (not found). 
          if(t != -1) {
            currentLine.add(new Token(t, str.toString()));
          } else {
            currentLine.add(new Token(34, str.toString())); // this may be incorrect to label this as an ERROR_TOKEN integer
                                                            // though it is an error- no custom lowercase variables
            return; 
          }

          continue;
        }

        //SPECIAL SYMBOLS
        // critical here to not stop when we see initial character.
        // this ensures when we read characters like '>' that it isn't tokenized until it is impossible for it to be '>='.
        // need to look an extra character ahead while also ensuring we aren't oobing
        
        // c1 & t1 are index + 1's char & type
        // c & t are 1st index char & type

        // index + 1 variables
        String c1;
        int t1; 

        if ( (index + 1) < line.length()) {
          c1 = line.substring(index, index + 2);
        } else {
          c1 = null; // oob
        }
        
        if(c1 != null) {
          t1 = getSymType(c1);
        } else {
          t1 = -1; // here t will be -1 if c1, the substring of c & c1, is not a valid symbol. i.e '>>' = -1 
        }

        if(t1 != -1) {
          currentLine.add(new Token(t1, c1));
          index += 2;
        } else {

          // single character symbol 
          int t = getSymType(String.valueOf(c));
          if (t != -1) {
            currentLine.add(new Token(t, String.valueOf(c)));
            index++;
          } else {
            currentLine.add(new Token(34, "ERROR_TOKEN"));
            return;
          }
        }
      }
    } catch (IOException err) {
      //eof = true; 
    }
  }
  
  // Returns the integer value of the given reserved word, str, returning -1 if it is not a reserved word.
  private int getResType(String str) {
    String[] reserved = {"program", "begin", "end", "int", "if", "then", "else", "while", "loop", "read", "write"};

    for(int i = 0; i < reserved.length; i++) {
      if(reserved[i].equals(str)) {
        return i + 1; 
      } 
    }
    
    return -1; // not found 
  }
  
  // Wanted to avoid making another array and searching through it. cleaner and is faster 
  // Returns the integer value of the given symbol, str, returning -1 if it is not found. 
  private int getSymType(String str) {
    switch(str) {
      case ";": return 12;
      case ",": return 13;
      case "=": return 14;
      case "!": return 15;
      case "[": return 16;
      case "]": return 17;
      case "&&": return 18;
      case "||": return 19;
      case "(": return 20;
      case ")": return 21;
      case "+": return 22;
      case "-": return 23;
      case "*": return 24;
      case "!=": return 25;
      case "==": return 26;
      case "<": return 27;
      case ">": return 28;
      case "<=": return 29;
      case ">=": return 30;
      default: return -1;
    }
  }
  
  // PUBLIC METHODS
  
  // Returns the token's type currently indexed by cursor. Does not progress cursor. 
  public int getToken() {
    return currentLine.get(cursor).t;
  }
  
  // Progresses cursor. Does not do so given an error or eof token. 
  // Once cursor is equal or past the size of the current line (starts = 0), new line
  public void skipToken() {
    int current = getToken();
    if(current == 33 || current == 34) return; // error or eof
    cursor++;
    if(cursor >= currentLine.size()) tokenizeLine();
  }
  
  // Returns the integer value of this. If this is not an integer token, error
  public int intVal() {
    if(getToken() != 31) {
      System.out.println("ERROR: intVal() received a non-integer");
      //System.exit(1); 'program will terminate'
    }
    return Integer.parseInt(currentLine.get(cursor).s);
  }

  // Returns the string value of this. If this is not an identifier token, error 
  public String idName() {
    if(getToken() != 32) {
      System.out.println("ERROR: idName() received a non-identifier");
      //System.exit(1);
    }
    return currentLine.get(cursor).s;
  }

  // Main function. 
  public static void main(String[] args) {
    if(args.length == 0) {
      System.out.println("ERROR: usage is 'java Tokenizer <file>'");
    }

    Tokenizer tk = new Tokenizer(args[0]);
    
    // scanning
    while(true) {
      int tkn = tk.getToken(); // current token

      System.out.print(tkn + " "); // display

      // if EOF or ERROR_TOKEN
      if(tkn == 33 || tkn == 34) {
        System.out.println(); 
        if(tkn == 33) System.out.println("EOF; TERMINATING");
        if(tkn == 34) System.out.println("INVALID TOKEN; TERMINATING");
        break; // stop scanning. 
      }
      tk.skipToken(); // go to next
    }
  }
} 
