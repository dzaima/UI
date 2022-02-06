package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
public class JavaLang extends Lang {
  public static LangState.Keywords keyw = new LangState.Keywords("_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "permits", "private", "protected", "public", "record", "return", "sealed", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "var", "void", "volatile", "while", "yield");
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff629755, // 2 string
    0xff6897BB, // 3 number
    0xffCC7832, // 4 keyword/string escape
    0xff81A2BE, // 5 class
  };
  public TextStyle[] styles;
  public TextStyle style(byte v) {
    return styles[v];
  }
  
  JavaState init;
  public JavaLang(Font f) {
    super(new JavaState());
    styles = Lang.colors(cols, f);
  }
  public Lang font(Font f) { return new JavaLang(f); }
  public LangState<?> init() { return init; }
  
  
  static class JavaState extends LangState<JavaState> {
    boolean mlc;
    int parenDelta, bracketDelta;
    
    public JavaState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      JavaState r = new JavaState();
      r.mlc = mlc;
      r.eval(sz, p, b);
      r.depthDelta = Math.min(Math.max(Math.max(r.depthDelta, 0), Math.max(r.parenDelta, r.bracketDelta)), 1);
      return r;
    }
    
    public void eval(int sz, char[] p, byte[] b) {
      Arrays.fill(b, (byte) -1); b[0] = 0;
      int i = 0;
      tk: while (i < sz) {
        int li = i;
        int c = p[i++];
        int n = i>=sz? 0 : p[i];
        if (c=='/' & n=='/') { b[li] = 1; return; }
        if (c=='/' & n=='*' | mlc) { b[li] = 1; if (mlc) i-= 2;
          mlc = true;
          while (true) {
            if (++i > sz-2) return;
            if (p[i]=='*' && p[i+1]=='/') {
              mlc = false;
              i+= 2; if (i>=sz) return;
              b[i] = 0;
              continue tk;
            }
          }
        } else if ((c>='0' & c<='9') | (c=='.' && n>='0' & n<='9')) {
          b[li] = 3;
          while (i<sz && p[i]>='0' & p[i]<='9' | p[i]=='.' | p[i]=='e' | p[i]=='-') i++;
          if (i>=sz) return;
          c = p[i];
          if (c=='f' | c=='F' | c=='d' | c=='D' | c=='l' | c=='L') i++;
          if (i>=sz) return;
          b[i] = 0;
        } else if (c=='"' | c=='\'') {
          b[li] = 2;
          while (true) {
            if (i>=sz) return;
            n = p[i++];
            if (n=='\\') { b[i-1]=4; i++; if(i<sz) b[i]=2; } // TODO \\u escapes
            else if (n==c) break;
          }
          if (i>=sz) return;
          b[i] = 0;
        } else if (nameChar(c)) {
          i--;
          do {
            if (++i>=sz) break;
          } while (nameChar(p[i])  |  p[i]>='0' & p[i]<='9');
          b[li] = (byte) (c<='Z'? 5 : keyw.has(p, li, i)? 4 : 0);
          if (i>=sz) return;
          b[i] = 0;
        }
        else if (c==';' | c==',') { b[li]=4; if(i<sz)b[i]=0; }
        else if (c=='(') parenDelta++; else if (c=='[') bracketDelta++; else if (c=='{') depthDelta++;
        else if (c==')') parenDelta--; else if (c==']') bracketDelta--; else if (c=='}') depthDelta--;
      }
    }
  
    public boolean equals(Object o) { return o instanceof JavaState && mlc==((JavaState) o).mlc; }
    public int hashCode() { return mlc?314159265:0; }
  }
  public static boolean nameChar(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
}