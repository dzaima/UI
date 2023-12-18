package dzaima.ui.node.types.editable.code.langs;

import java.util.*;

public abstract class LangState<T extends LangState<T>> {
  public int depthDelta; // how many more/less spaces to indent next line with 
  
  public abstract T after(int sz, char[] p, byte[] b);
  
  
  public abstract boolean equals(Object obj);
  public abstract int hashCode();
  
  public static class Keywords extends HashSet<Keywords.KW> {
    public Keywords(String... ss) {
      for (String c : ss) add(new KW(c.toCharArray()));
    }
    
    public boolean has(char[] p, int s, int e) {
      return contains(new KW(Arrays.copyOfRange(p, s, e)));
    }
    public boolean has(char[] str) {
      return contains(new KW(str));
    }
    
    static class KW {
      char[] is;
      int hc;
      KW(char[] is) {
        this.is = is;
        hc = Arrays.hashCode(is);
      }
      
      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // don't care
      public boolean equals(Object o) {
        return Arrays.equals(is, ((KW) o).is);
      }
      
      public int hashCode() {
        return hc;
      }
    }
  }
  public static class Chars extends HashSet<Integer> {
    public Chars(String s) {
      int i = 0;
      while (i < s.length()) {
        int p = s.codePointAt(i);
        add(p);
        i+= Character.charCount(p);
      }
    }
  }
}
