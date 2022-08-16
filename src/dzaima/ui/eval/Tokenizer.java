package dzaima.ui.eval;

import dzaima.ui.eval.Token.*;
import dzaima.utils.*;

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
      } else if (dig(c) | (c=='.' & i<s.length() && dig(s.charAt(i))) | c=='-') {
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
        String v = s.substring(li, i);
        String[] parts = Tools.split(v, '.');
        for (int j = 0; j < parts.length; j++) {
          String p = parts[j];
          if (p.length() == 0) throw err("Empty dot-separated segment", i);
          if (!nameS(p.charAt(0))) throw err("Name starts with number", i);
          int idx = p.indexOf('$');
          if (idx>0) throw err("'$' in the middle of name", i);
          if (idx==0 && j!=parts.length-1) throw err("'$' can only be used in the last item of a dot chain", i);
        }
        res.add(new NameTok(li, v, parts[parts.length-1].charAt(0)=='$'));
      } else if (c=='"') {
        int li = i;
        StringBuilder b = new StringBuilder();
        while (true) {
          c = s.charAt(i++);
          if (c=='"') break;
          if (c=='\\') {
            if (i>=s.length()) throw err("Tokenize error: unfinished string", i);
            c = s.charAt(i++);
            if (c=='\\') b.append('\\');
            else if (c=='"') b.append('"');
            else if (c=='n') b.append('\n');
            else if (c=='r') b.append('\r');
            else if (c=='t') b.append('\t');
            else throw err("Tokenize error: string escapes not finished", i);
          } else b.append(c);
        }
        res.add(new StrTok(li, b.toString()));
      } else if (c=='#') {
        int li = i;
        while (i<s.length() && hex(s.charAt(i))) i++;
        res.add(new ColorTok(li, s.substring(li, i)));
      } else if (c=='/') {
        if (i>=s.length()) throw err("Tokenize error: ending with '/'", i);
        char n = s.charAt(i++);
        if (n=='/') {
          while (i<s.length() && !ln(s.charAt(i))) i++;
        } else if (n=='*') {
          i++;
          do {
            if (i>=s.length()) throw err("Tokenize error: unfinished '/*' comment", i);
            if (s.charAt(i-1)=='*' && s.charAt(i)=='/') break;
            i++;
          } while (true);
          i++;
        } else throw err("Tokenize error: slash not followed by '/' or '*'", i);
      } else throw err("Tokenize error: failed to parse chr '"+c+"'", i);
    }
    if (depth!=0) throw err("Parse error: mismatched brackets", i);
    res.add(new EOFTok(s.length()));
    return res;
  }
  
  private static RuntimeException err(String msg, int off) {
    return new Prs.ParserException(msg+" at "+off);
  }
  
  
  private static boolean nameS(char c) {
    return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='.'  |  c=='$';
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
