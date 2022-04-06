package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
public class SingeliLang extends Lang {
  public static LangState.Keywords types = new LangState.Keywords(
    "void","i8","i16","i32","i64","u1","u8","u16","u32","u64","f32","f64","Size"
  );
  public static LangState.Keywords keyw = new LangState.Keywords(
    "def","include","do","while","if","else","oper","over","from","to","return","and","or","in"
  );
  
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xffE7DB74, // 2 string
    0xff9e7cdd, // 3 number
    0xffe08031, // 4 keyword
    0xff81A2BE, // 5 type
    0xffD2D2D2, // 6 invocation
    0xff72c3d3, // 7 builtin
  };
  public TextStyle[] styles;
  public TextStyle style(byte v) {
    return styles[v];
  }
  
  SingeliState init;
  public SingeliLang(Font f) {
    super(new SingeliState());
    styles = Lang.colors(cols, f);
  }
  public Lang font(Font f) { return new SingeliLang(f); }
  public LangState<?> init() { return init; }
  
  
  static class SingeliState extends LangState<SingeliState> {
    boolean mlc;
    
    public SingeliState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      SingeliState r = new SingeliState();
      r.mlc = mlc;
      r.eval(sz, p, b);
      // r.depthDelta = Math.min(Math.max(Math.max(r.depthDelta, 0), Math.max(r.parenDelta, r.bracketDelta)), 1);
      return r;
    }
    
    public void eval(int sz, char[] s, byte[] r) {
      if (sz==0) return;
      Arrays.fill(r, (byte) -1); r[0] = 0;
      int i = 0;
      while (i < sz) {
        int li = i;
        int c = s[i];
        switch (c) {
          case '#': { r[li] = 1; return; }
          case '\'':
            r[i++] = 2;
            while (i<sz && s[i]!='\'') i++;
            i++;
            break;
          case '@':
            r[i++] = 4;
            while (i<sz && nameM(s[i])) i++;
            break;
          case'-':case'$':case'*':case'<':case'>':case'+':case'/':case'%':case'&':case'|':case'^':case'~':case'=':case':':
            r[i++] = 7;
            break;
          case'0':case'1':case'2':case'3':case'4':case'5':case'6':case'7':case'8':case'9':case'.':
            r[i++] = 3;
            while (i<sz && (nameM(s[i]) || s[i]=='.')) i++;
            break;
          case '_':
            if (i+1<sz && s[i+1]=='_') {
              r[i++] = 7;
              while (i<sz && nameM(s[i])) i++;
              break;
            }
            /* else fallthrough */
          default:
            if (nameS(c)) {
              while (i<sz && nameM(s[i])) i++;
              byte t0 = (byte) (types.has(s, li, i)? 5 : keyw.has(s, li, i)? 4 : 0);
              if (t0==0 && i<sz) {
                while (i<sz && ws(s[i])) i++;
                if (i<sz && s[i]=='{') t0 = 6;
              }
              r[li] = t0;
            } else {
              r[i++] = 0;
            }
            break;
        }
      }
    }
    
    public boolean equals(Object o) { return o instanceof SingeliState && mlc==((SingeliState) o).mlc; }
    public int hashCode() { return mlc?314159265:0; }
  }
  public static boolean ws(int c) { return c==' '|c=='\n'; }
  public static boolean dig(int c) { return c>='0' & c<='9'; }
  public static boolean nameS(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
  public static boolean nameM(int c) { return nameS(c) || dig(c); }
}