package dzaima.ui.node.types.editable.code.langs;


import dzaima.ui.gui.Font;
import dzaima.ui.node.types.editable.code.*;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public class BQNLang extends Lang {
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff6A9FFB, // 2 string
    0xffff6E6E, // 3 number
    0xff57d657, // 4 function
    0xffEB60DB, // 5 1-modifier
    0xffFFDD66, // 6 2-modifier
    0xffAA77BB, // 7 block
    0xffFFFF00, // 8 control
    0xffDD99FF, // 9 arr
  };
  public BQNLang() {
    super(new BQNState());
  }
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  
  public static int codePointAt(char[] a, int i, int sz) {
    char c1 = a[i];
    if (Character.isHighSurrogate(c1) && ++i<sz && Character.isLowSurrogate(a[i])) return Character.toCodePoint(c1, a[i]);
    return c1;
  }
  
  private static class BQNState extends LangState<BQNState> {
    public Chars fns = new Chars("!+-×÷⋆*√⌊⌈∧∨¬|=≠≤<>≥≡≢⊣⊢⥊∾≍⋈↑↓↕⌽⍉/⍋⍒⊏⊑⊐⊒∊⍷⊔«»⍎⍕");
    public Chars md1 = new Chars("`˜˘¨⁼⌜´˝˙");
    public Chars md2 = new Chars("∘⊸⟜○⌾⎉⚇⍟⊘◶⎊");
    public Chars block = new Chars("𝕨𝕩𝔽𝔾𝕎𝕏𝕗𝕘𝕣ℝ𝕤𝕊{}:");
    public Chars arr = new Chars("·⍬‿⦃⦄⟨⟩@");
    public BQNState after(int sz, char[] p, byte[] b) { // TODO depthDelta
      int i = 0;
      while (i < sz) {
        int li = i;
        int c = codePointAt(p, i, sz);
        i+= Character.charCount(c);
        if (c=='#') { Arrays.fill(b, li, sz, (byte) 1); break; }
        else if (c=='{' | c=='}') { b[li] = 7; /*depthDelta+= (c=='{'?1:-1);*/ }
        else if (block.contains(c)) { b[li] = 7; if (i==li+2) b[li+1]=7; }
        else if (c=='⋄' | c=='←' | c=='↩' | c==',' || c=='→' | c=='⇐')  b[li] = 8;
        else if (arr.contains(c))  b[li] = 9;
        else if (c=='\'' && li+2<sz && p[li+2]=='\'') { b[li] = 2; b[li+1]=b[li+2]=-1; i+= 2; }
        else if (c=='\"') { b[li]=2; if(i<sz) b[i++]=2; while(i<sz && p[i-1]!='\"') { b[i]=2; i++; } }
        else if (fns.contains(c)) b[li] = 4;
        else if (md1.contains(c)) b[li] = 5;
        else if (md2.contains(c)) b[li] = 6;
        else if (isDig(c) | c=='.') {
          if (c=='.' && (i>=sz || !isDig(p[i]))) b[li] = 6;
          else {
            while (i<sz && (isDig(p[i]) || p[i]=='_' || p[i]=='.' || p[i]=='e' || p[i]=='E')) i++;
            Arrays.fill(b, li, i, (byte) 3);
          }
        }
        else if (nameChar(c) | c=='•') {
          if (c!='•') i--; // set up i to be the first name character
          if (i>=sz) continue;
          int c0 = p[i];
          while (i<sz && nameChar(p[i]) | p[i]>='0' & p[i]<='9') i++;
          int c1 = p[i-1];
          Arrays.fill(b, li, i, (byte) -1);
          b[li] = (byte) (c0=='_'? (c1=='_'? 6 : 5) : (c0>='A'&c0<='Z'? 4 : 0));
        } else b[li] = 0;
      }
      return this;
    }
    public static boolean nameChar(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
    
    public boolean equals(Object obj) { return this==obj; }
    public int hashCode() { return 0; }
  }
  
  private static boolean isDig(int c) {
    return c>='0'&c<='9' | c=='∞' | c=='¯' | c=='∞' | c=='π';
  }
}
