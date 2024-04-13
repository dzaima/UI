package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
import java.util.regex.Pattern;

public abstract class AsmLang extends Lang {
  protected abstract boolean isReg(String s);
  protected abstract boolean isKW(String s);
  protected abstract boolean isPrefix(String s);
  
  public static final AsmLang GENERIC = new AsmLang() {
    protected boolean isReg(String s) { return false; }
    protected boolean isKW(String s) { return false; }
    protected boolean isPrefix(String s) { return false; }
  };
  
  public static final AsmLang X86 = new AsmLang() {
    private final LangState.Keywords sizes = new LangState.Keywords("ptr","byte","word","dword","qword","tword","mmword","xmmword","ymmword","zmmword");
    private final LangState.Keywords prefixes = new LangState.Keywords(
      "data16", "data32", "addr16", "addr32", "rex64",
      "xacquire", "xrelease", "acquire", "release",
      "lock", "rep", "repe", "repz", "repne", "repnz", "notrack"
    );
    private final Pattern regs = Pattern.compile("%?(([re]?(ip|ax|bx|cx|dx|si|di|sp|bp))|[abcd][hl]|(si|di|sp|bp)l|r(8|9|1[0-5])[dwb]?|[cdsefg]s|[xyz]mm([12]?[0-9]|3[01])|[cdt]r[0-9]+|k[0-7])");
    protected boolean isReg(String s) { return regs.matcher(s).matches(); }
    protected boolean isKW(String s) { return sizes.has(s.toLowerCase().toCharArray()); }
    protected boolean isPrefix(String s) { return prefixes.has(s.toLowerCase().toCharArray()); }
  };
  
  public static final AsmLang RISCV = new AsmLang() {
    private final Pattern regs = Pattern.compile("[xfv]([12]?[0-9]|3[01])|v0\\.t|zero|ra|[sgf]p|tp|t[0-6]|s[0-9]|s1[01]|f?a[0-7]|f[ts]([0-9]|1[01])");
    private final LangState.Keywords kw = new LangState.Keywords("%pcrel_hi", "%pcrel_lo", "%got_pcrel_hi");
    protected boolean isReg(String s) { return regs.matcher(s).matches(); }
    protected boolean isKW(String s) { return kw.has(s.toCharArray()); }
    protected boolean isPrefix(String s) { return false; }
  };
  
  public static final AsmLang AARCH64 = new AsmLang() {
    private final Pattern regs = Pattern.compile("sp|pc|cpsr|fpsr|fpcr|([vqdshbz]([12]?[0-9]|3[01])|p([0-9]|1[0-5]))(\\.[0-9]*[bhsd])?|[wxr]([012]?[0-9]|30|zr)");
    protected boolean isReg(String s) { return regs.matcher(s).matches(); }
    protected boolean isKW(String s) { return s.equals("lo12"); }
    protected boolean isPrefix(String s) { return false; }
  };
  
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff7AAFDB, // 2 instruction - add, mov, etc
    0xffCC6666, // 3 QWORD PTR etc
    0xff3DC9B0, // 4 labels
    0xffB5CEA8, // 5 numbers
    0xffCE9178, // 6 strings
    0xff5B73B2, // 7 registers
  };
  public AsmLang() {
    super(new AsmState(null));
    ((AsmState) init).l = this;
  }
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  
  static class AsmState extends LangState<AsmLang.AsmState> {
    private AsmLang l;
    private AsmState(AsmLang l) { this.l = l; }
    
    public AsmLang.AsmState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      AsmLang.AsmState r = new AsmLang.AsmState(l);
      r.eval(sz, p, b);
      return r;
    }
    
    public void eval(int sz, char[] s, byte[] r) {
      if (sz==0) return;
      Arrays.fill(r, (byte) -1); r[0] = 0;
      boolean firstWord = true;
      int i = 0;
      while (i < sz) {
        int li = i;
        int c = s[i];
        switch (c) {
          case '\'': case '"':
            r[i++] = 6;
            while (i<sz) {
              if (s[i]==c) break;
              i+= s[i]=='\\'? 2 : 1;
            }
            i++;
            break;
          case '<':
            r[i++] = 6;
            while (i<sz && s[i-1]!='>') i++;
            break;
          case '/':
            if (i!=s.length-1 && s[i+1]=='/') { r[li] = 1; return; }
            else r[i++] = 0;
            break;
          case '#': if (i!=s.length-1 && !dig(s[i+1]) && s[i+1]!='-') { r[li] = 1; return; }
            // fallthrough
          case'0':case'1':case'2':case'3':case'4':case'5':case'6':case'7':case'8':case'9':case'.':
            if (c!='.' || i+1>=sz || dig(s[i+1])) {
              r[i++] = 5;
              while (i<sz && ((c!='.' && nameM(s[i])) || s[i]=='.')) i++;
              break;
            }
            // fallthrough
          default:
            if (nameS(c) || c=='.' || c=='%') {
              i++;
              while (i<sz && (nameM(s[i]) || s[i]=='.')) i++;
              
              String str = new String(s, li, i-li);
              
              byte t = (byte) (firstWord? 2 : 4);
              if (firstWord && i<sz && s[i]==':') { t = 4; i++; }
              else if (l.isKW(str)) t = 3;
              else if (l.isReg(str)) t = 7;
              else {
                if (!firstWord || !l.isPrefix(str)) firstWord = false;
              }
              r[li] = t;
            } else {
              r[i++] = 0;
            }
            break;
        }
      }
    }
    
    public boolean equals(Object o) { return o instanceof AsmLang.AsmState; }
    public int hashCode() { return 0; }
  }
  public static boolean ws(int c) { return c==' '|c=='\n'; }
  public static boolean dig(int c) { return c>='0' & c<='9'; }
  public static boolean nameS(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
  public static boolean nameM(int c) { return nameS(c) || dig(c) || c=='@'; }
}
