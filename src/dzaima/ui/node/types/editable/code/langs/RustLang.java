package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
public class RustLang extends Lang {
  public static LangState.Keywords keyw = new LangState.Keywords(
    "as","break","const","continue","crate","else","enum","extern","false","fn","for","if",
    "impl","in","let","loop","match","mod","move","mut","pub","ref","return","self","Self",
    "static","struct","super","trait","true","type","unsafe","use","where","while",
    "async","await","dyn"
  );
  public static LangState.Keywords types = new LangState.Keywords(
    "bool","char","fn",
    "i8","i16","i32","i64","i128","isize",
    "u8","u16","u32","u64","u128","usize",
    "f32","f64"
  );
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff629755, // 2 string
    0xff6897BB, // 3 number
    0xffCC7832, // 4 keyword/string escape
    0xff81A2BE, // 5 class
  };
  public RustLang() {
    super(new RustState());
  }
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  
  static class RustState extends LangState<RustState> {
    boolean mlc;
    int parenDelta, bracketDelta;
    
    public RustState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      RustState r = new RustState();
      r.mlc = mlc;
      r.eval(sz, p, b);
      r.depthDelta = Math.min(Math.max(Math.max(r.depthDelta, 0), Math.max(r.parenDelta, r.bracketDelta)), 1);
      return r;
    }
    
    public void eval(int sz, char[] p, byte[] b) {
      Arrays.fill(b, (byte) -1); b[0] = 0;
      int i = 0;
      if (i<sz) b[i] = 0;
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
          
          if      (n=='b') { i++; while (i<sz  &&  (p[i]=='0' | p[i]=='1')) i++; }
          else if (n=='x') { i++; while (i<sz  &&  (p[i]>='0' & p[i]<='9'  |  p[i]>='a' & p[i]<='f'  |  p[i]>='A' & p[i]<='F')) i++; }
          else while (i<sz && p[i]>='0' & p[i]<='9' | p[i]=='.' | p[i]=='e' | p[i]=='E' | p[i]=='-') i++;
          
          while (i<sz  &&  (c=p[i])=='f' | c=='F' | c=='d' | c=='D' | c=='u' | c=='U' | c=='l' | c=='L') i++;
          if (i>=sz) return;
          b[i] = 0;
        } else if (c=='\'') {
          if (n=='\\' || (i+1 < sz && p[i+1]=='\'')) { // character literal
            b[li] = 2;
            if (n=='\\') i+= 1;
            do {
              if (++i>=sz) break;
            } while (p[i]!='\'');
            i++;
          } else { // lifetime
            b[li] = 4;
            do {
              if (++i>=sz) break;
            } while (nameMid(p[i]));
          }
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
        } else if (nameStart(c)) {
          i--;
          do {
            if (++i>=sz) break;
          } while (nameMid(p[i]));
          b[li] = (byte) (types.has(p,li,i)? 5 : keyw.has(p,li,i)? 4 : 0);
          if (i>=sz) return;
          b[i] = 0;
        }
        else if (c==';' | c==',' | c=='#') { b[li]=4; if(i<sz)b[i]=0; }
        else if (c=='(') parenDelta++; else if (c=='[') bracketDelta++; else if (c=='{') depthDelta++;
        else if (c==')') parenDelta--; else if (c==']') bracketDelta--; else if (c=='}') depthDelta--;
      }
    }
    
    public boolean equals(Object o) { return o instanceof RustState && mlc==((RustState) o).mlc; }
    public int hashCode() { return mlc?314159265:0; }
  }
  public static boolean nameStart(int c) { return (c>='a' & c<='z')  |  (c>='A' & c<='Z')  |  c=='_'; }
  public static boolean nameMid(int c) { return nameStart(c)  |  (c>='0' & c<='9'); }
}