package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public class APLLang extends Lang {
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xffDDAAEE, // 2 string
    0xffAA88BB, // 3 number
    0xff00ff00, // 4 fn
    0xffFF9955, // 5 mop
    0xffFFDD66, // 6 dop
    0xffAA77BB, // 7 dfn
    0xffFFFF00, // 8 diamond
    0xffDD99FF, // 9 arr
  };
  public APLLang() {
    super(new APLState());
  }
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  
  private static class APLState extends LangState<APLState> {
    public Chars fns = new Chars("^⌹⍳⍴!%*+,-<=>?|~⊢⊣⌷≤≥≠∨∧÷×∊↑↓○⌈⌊⊂⊃∩∪⊥⊤⍱⍲⍒⍋⍉⌽⊖⍟⍕⍎⍪≡≢⍷⍸⊆⊇⍧⍮√ϼ…");
    public Chars mops = new Chars("¨⍨⌸⍁⍩ᑈᐵ⌶/\\&⌿⍀");
    public Chars dops = new Chars(".@∘⌺⍫⍣⍢⍤⍛⍡⍥⍠");
    public APLState after(int sz, char[] p, byte[] b) { // TODO depthDelta
      int i = 0;
      while (i < sz) {
        int li = i;
        int c = p[i++];
        if (c=='⍝') { Arrays.fill(b, li, sz, (byte) 1); break; }
        else if (c>='0'&c<='9' | c=='¯' | c=='∞' | c=='.') {
          if (c=='.' && (i>=sz || !isDig(p[i]))) b[li] = 6;
          else {
            while (i<sz && (isDig(p[i]) || p[i]=='.' || p[i]=='e' || p[i]=='E')) i++;
            Arrays.fill(b, li, i, (byte) 3);
          }
        }
        else if (c=='{' | c=='}') { b[li] = 7; depthDelta+= (c=='{'?1:-1); }
        else if (c=='⍺' | c=='⍵' | c=='⍶' | c=='⍹')  b[li] = 7;
        else if (c=='⋄' | c=='←' | c=='→')  b[li] = 8;
        else if (c=='#' | c=='⍬')  b[li] = 9;
        else if (c=='\'') {
          while (i<sz && p[i]!='\'') i++;
          Arrays.fill(b, li, i>=sz? i : i+1, (byte) 2);
          i++;
        } else if (fns.contains(c))  b[li] = 4;
        else if (mops.contains(c)) b[li] = 5;
        else if (dops.contains(c)) b[li] = 6;
        else b[li] = 0;
      }
      return this;
    }
    
    public boolean equals(Object obj) { return this==obj; }
    public int hashCode() { return 0; }
  }
  private static boolean isDig(int c) {
    return c>='0'&c<='9' | c=='∞' | c=='¯' | c=='∞';
  }
}
