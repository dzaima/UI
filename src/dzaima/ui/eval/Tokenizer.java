package dzaima.ui.eval;

import dzaima.ui.eval.Token.*;
import dzaima.utils.Vec;

public class Tokenizer {
  
  public static Vec<Token> tk(String s) {
    int i = 0;
    Vec<Token> res = new Vec<>(s.length()/4);
    int depth = 0;
    while (i < s.length()) {
      char c = s.charAt(i++);
      if (c==' ' | c=='\t' | ln(c)) continue;
      
      
      if (c=='{' | c=='}' | c==':' | c=='=') {
        depth+= (c=='{'?1:0)-(c=='}'?1:0);
        res.add(new Token(i, c));
      } else if (dig(c) | (c=='.' & i<s.length() && dig(s.charAt(i)))) {
        int li = i-1;
        while (i<s.length() && dig(s.charAt(i))) i++;
        if (i<s.length() && s.charAt(i)=='.') {
          i++;
          while (i<s.length() && dig(s.charAt(i))) i++;
        }
        double d = Double.parseDouble(s.substring(li, i));
        if (i<s.length() && nameS(s.charAt(i))) {
          int mi = i;
          while (i<s.length() && nameS(s.charAt(i))) i++;
          res.add(new NumTok(li, d, s.substring(mi, i)));
        } else {
          res.add(new NumTok(li, d, ""));
        }
        
      } else if (nameS(c)) {
        int li = i-1;
        while (i<s.length() && nameM(s.charAt(i))) i++;
        // TODO properly.parse.dot.chains
        res.add(new NameTok(li, s.substring(li, i)));
      } else if (c=='"') {
        int li = i;
        StringBuilder b = new StringBuilder();
        while (true) {
          c = s.charAt(i++);
          if (c=='"') break;
          if (c=='\\') {
            if (i>=s.length()) throw new RuntimeException("Tokenize error: unfinished string");
            c = s.charAt(i++);
            if (c=='\\') b.append('\\');
            else if (c=='"') b.append('"');
            else if (c=='n') b.append('\n');
            else if (c=='r') b.append('\r');
            else if (c=='t') b.append('\t');
            else throw new RuntimeException("Tokenize error: string escapes not finished");
          } else b.append(c);
        }
        res.add(new StrTok(li, b.toString()));
      } else if (c=='#') {
        int li = i;
        while (i<s.length() && hex(s.charAt(i))) i++;
        res.add(new ColorTok(li, s.substring(li, i)));
      } else if (c=='/') {
        if (i>=s.length()) throw new RuntimeException("Tokenize error: ending with '/'");
        char n = s.charAt(i++);
        if (n=='/') {
          while (i<s.length() && !ln(s.charAt(i))) i++;
        } else if (n=='*') {
          i++;
          do {
            if (i>=s.length()) throw new RuntimeException("Tokenize error: unfinished '/*' comment");
            if (s.charAt(i-1)=='*' && s.charAt(i)=='/') break;
            i++;
          } while (true);
          i++;
        } else throw new RuntimeException("Tokenize error: slash not followed by '/' or '*'");
      } else throw new RuntimeException("Tokenize error: failed to parse chr '"+c+"'");
    }
    if (depth!=0) throw new RuntimeException("Parse error: mismatched brackets");
    res.add(new EOFTok(s.length()));
    return res;
  }
  private static boolean nameS(char c) {
    return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='.';
  }
  private static boolean nameM(char c) {
    return nameS(c) | dig(c);
  }
  private static boolean hex(char c) {
    return c>='a' & c<='f'  |  c>='A' & c<='F'  |  c>='0' & c<='9';
  }
  private static boolean ln(char c) {
    return c=='\n' | c=='\r';
  }
  private static boolean dig(char c) {
    return c>='0' & c<='9';
  }
}
