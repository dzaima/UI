package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import dzaima.ui.node.types.editable.code.*;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
public class CLang extends Lang {
  public static LangState.Keywords keyw = new LangState.Keywords(
    "_Alignas","_Alignof","_Atomic","_Generic","_Noreturn","_Static_assert","_Thread_local",
    "auto","break","case","const","continue","default","do","else","enum",
    "extern","for","goto","if","inline","register","restrict","return","sizeof",
    "static","struct","switch","typedef","union","volatile","while"
  );
  public static LangState.Keywords types = new LangState.Keywords(
    "_Bool","_Complex","_Decimal128","_Decimal32","_Decimal64","_Imaginary",
    "signed","unsigned","void","char","short","int","long","double","float",
    "bool","i8","i16","i32","i64","u8","u16","u32","u64","f32","f64"
  );
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff629755, // 2 string
    0xff6897BB, // 3 number
    0xffCC7832, // 4 keyword/string escape
    0xff81A2BE, // 5 class
  };
  public CLang() {
    super(new CState());
  }
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  
  static class CState extends LangState<CState> {
    boolean mlc;
    int parenDelta, bracketDelta;
    
    public CState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      CState r = new CState();
      r.mlc = mlc;
      r.eval(sz, p, b);
      r.depthDelta = Math.min(Math.max(Math.max(r.depthDelta, 0), Math.max(r.parenDelta, r.bracketDelta)), 1);
      return r;
    }
    
    public void eval(int sz, char[] p, byte[] b) {
      Arrays.fill(b, (byte) -1); b[0] = 0;
      int i = 0;
      if (!mlc) while (i < sz) {
        char c = p[i];
        if (c=='#') {
          b[i] = 4;
          i++;
          while (i<sz && nameChar(p[i])) i++;
          break;
        }
        if (c!=' ' && c!='\t') break;
        i++;
      }
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
          b[li] = (byte) (types.has(p,li,i)? 5 : keyw.has(p,li,i)? 4 : 0);
          if (i>=sz) return;
          b[i] = 0;
        }
        else if (c==';' | c==',') { b[li]=4; if(i<sz)b[i]=0; }
        else if (c=='(') parenDelta++; else if (c=='[') bracketDelta++; else if (c=='{') depthDelta++;
        else if (c==')') parenDelta--; else if (c==']') bracketDelta--; else if (c=='}') depthDelta--;
      }
    }
    
    public boolean equals(Object o) { return o instanceof CState && mlc==((CState) o).mlc; }
    public int hashCode() { return mlc?314159265:0; }
  }
  public static boolean nameChar(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
}