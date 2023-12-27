package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;
import io.github.humbleui.skija.paragraph.*;

import java.util.Arrays;

public class StringNode extends InlineNode {
  public final String s;
  public Font f;
  public int colFG, colBG;
  public static final short FL_SEL = F_C1;
  public static final short FLS_SEL = FL_SEL;
  
  public StringNode(Ctx ctx, String s) {
    super(ctx, Props.none());
    assert s!=null;
    this.s = s;
  }
  
  
  private String[] lns; // non-inline only; TODO reuse words maybe?
  public Word[] words; // inline only
  public short asc;
  private XY sz; // TODO not
  private int maxW;
  
  public void propsUpd() { super.propsUpd();
    if (p instanceof InlineNode) {
      Font cf = ((InlineNode) p).getFont();
      if (words==null) words = words(s);
      else for (Word w : words) w.overkill = null;
      maxW = 0;
      int lnW = 0;
      for (Word c : words) {
        lnW+= cf.width(c.s);
        if (c.f(Word.F_LN)) { maxW=Math.max(maxW,lnW); lnW=0; }
      }
      maxW = Math.max(maxW, lnW) + 1;
      sz = new XY(Math.min(gc.em, maxW), -1);
      lns = null;
    } else {
      words = null;
      if (lns==null) {
        lns = (s.replace("\t", "  ")+"\na").split("\\r?\\n");
        lns = Arrays.copyOf(lns, lns.length-1);
      }
      String family = gc.strD(p, "family", "Arial");
      int tsz = gc.emD(p, "tsz", 1);
      colFG = gc.col(p, "color", "str.color");
      colBG = 0;
      int mode = 0;
      if(gc.boolD(p, "bold"   , false)) mode|= Typeface.BOLD;
      if(gc.boolD(p, "italics", false)) mode|= Typeface.ITALICS;
      f = Typeface.of(family).sizeMode(tsz, mode);
      int w = 0;
      for (String c : lns) w = Math.max(w, f.width(c));
      sz = new XY(w, lns.length * f.hi);
      maxW = w;
    }
  }
  
  public static boolean PARAGRAPH_TEXT = true;
  public static void text(Graphics g, String s, Font f, int col, float x, float y) {
    if (PARAGRAPH_TEXT) g.textP(s, f, x, y, col);
    else g.text(s, f, x, y, col);
  }
  public static void text(Graphics g, String s, StringNode n, float x, float y) {
    text(g, s, n.f, n.colFG, x, y);
  }
  
  public static void text(Graphics g, Word w, StringNode n, float x, float y) {
    if (PARAGRAPH_TEXT && !n.f.hasAll(w.s)) {
      Paragraph r = w.overkill;
      if (r==null) w.overkill = r = w.buildPara(n);
      r.paint(g.canvas, x, y-n.f.asc);
    } else g.text(w.s, n.f, x, y, n.colFG);
  }
  
  public void drawC(Graphics g) {
    int fh = f.hi;
    if (words!=null) {
      if (g.clip!=null) if (g.clip.ey<sY1 || g.clip.sy>sY1+h) return;
      for (Word c : words) {
        int y = c.y+f.ascI;
        if (c.split == null) {
          text(g, c, this, c.x, y);
        } else {
          int i = 0;
          text(g, c.split[i++], this, c.x, y);
          if (c.f(Word.F_SL)) y = sY2+f.ascI;
          else y+= fh;
          while (i < c.split.length-1) {
            text(g, c.split[i++], this, 0, y);
            y+= fh;
          }
          if (c.f(Word.F_EL)) y = eY1+asc+f.ascI;
          text(g, c.split[i], this, 0, y);
        }
      }
    } else {
      if (g.clip!=null) if (g.clip.ey<0 || g.clip.sy>sz.y) return;
      int y = 0;
      for (String c : lns) {
        text(g, c, this, 0, f.ascI+y);
        y+= fh;
      }
    }
  }
  
  public void bg(Graphics g, boolean full) {
    pbg(g, full);
    XY sel = ctx.win().stringSelection(this);
    int ss = sel==null? -1 : sel.x;
    int se = sel==null? -1 : sel.y;
    if (words!=null && (Tools.vs(colBG) || sel!=null)) {
      int fh = f.hi;
      if (g.clip!=null) if (g.clip.ey<sY1 || g.clip.sy>sY1+h) return;
      int cs = 0;
      for (Word c : words) {
        int x = (int) c.x;
        int y = c.y;
        if (c.split == null) {
          cs = bgRect(g, ss, se, cs, x, y, c.s, (int) Math.ceil(c.w), fh);
        } else {
          int i = 0;
          cs = bgRect(g, ss, se, cs, x, y, c.split[i], f.width(c.split[i]), fh); i++;
          if (c.f(Word.F_SL)) y = sY2;
          else y+= fh;
          while (i < c.split.length-1) {
            cs = bgRect(g, ss, se, cs, 0, y, c.split[i], f.width(c.split[i]), fh); i++;
            y+= fh;
          }
          if (c.f(Word.F_EL)) y = eY1+asc;
          cs = bgRect(g, ss, se, cs, 0, y, c.split[i], f.width(c.split[i]), fh);
        }
      }
    }
  }
  private int bgRect(Graphics g, int ss, int se, int cs, int x, int y, String s, int w, int h) {
    int ce = cs+s.length();
    g.rectWH(x, y, w, h, colBG);
    if (!(cs>se || ce<ss)) {
      int os = f.width(s.substring(0, Math.max(0, ss-cs)));
      int oe = f.width(s.substring(0, Math.min(s.length(), se-cs)));
      g.rect(x+os, y, x+oe, y+h, gc.getProp("text.bgSel").col());
    }
    return ce;
  }
  
  public int minW(     ) { assert visible; return sz.x; }
  public int maxW(     ) { assert visible; return maxW; }
  public int minH(int w) { assert visible; return sz.y; }
  
  public void addInline(InlineSolver sv) { // TODO this probably deserves to be optimized a lot
    boolean mut = sv.resize;
    if (sv.f != f) {
      f = sv.f;
      for (Word c : words) c.setFont(f);
    }
    if (words.length==0) return;
    
    int a = f.ascI;
    int b = f.dscI;
    int li = 0;
    boolean first = true;
    for (int i = 0; i < words.length; i++) {
      Word c = words[i];
      if (mut) {
        c.setFlag(Word.F_EL, true); // cleared by newline calls
      }
      if (mut) c.split = null;
      if (sv.x+c.w >= sv.w) {
        if (c.w >= sv.w && c.s.length()>1) { // break up word
          c.setFlag(Word.F_SL, first);
          int x0 = search(c.s, 0, sv.w-sv.x);
          Vec<String> spl = new Vec<>();
          spl.add(c.s.substring(0, x0)); // TODO maybe do something special if this is a length zero string? (make sure to correct F_SL if so)
          while (x0 != c.s.length()) {
            int x1 = Math.max(search(c.s, x0, sv.w), x0+1);
            spl.add(c.s.substring(x0, x1));
            x0 = x1;
          }
          sv.ab(a, b);
          if (spl.sz==1) spl.add(""); // just in case
          if (mut) {
            c.split = spl.toArray(new String[0]);
            c.x = sv.x;
            c.y = (short) (sv.y+sv.a-a);
          }
          li = newline(li, i, sv, a, b, mut); first = false;
          sv.x = f.width(spl.peek());
          sv.y+= f.hi*(spl.sz-2);
          continue;
        } else { // don't break word; flow to next line and continue as normally
          if (i!=0) sv.ab(a, b);
          li = newline(li, i, sv, a, b, mut); first = false;
        }
      }
      if (mut) {
        c.x = sv.x;
        c.y = (short) sv.y;
        c.setFlag(Word.F_SL, first);
      }
      sv.x+= c.w;
      if (c.f(Word.F_LN)) { sv.ab(a, b); li = newline(li, i+1, sv, a, b, mut); first = false; }
    }
    sv.ab(a, b);
    
    colFG = sv.tcol;
    colBG = sv.tbg;
    if (mut) mRedraw();
  }
  protected void baseline(int asc, int dsc, int h) {
    this.asc = (short)(asc-f.ascI);
    for (Word w : words) if (w.f(Word.F_EL) && w.split==null) w.y+= asc-f.ascI;
  }
  private int newline(int s, int e, InlineSolver sv, int a, int b, boolean mut) {
    int dy = sv.a-a;
    assert s==e || dy>=0 : this.s+": "+s+" "+e+" "+sv.a+" "+a;
    if (mut) for (int i = s; i < e; i++) {
      words[i].y+= dy;
      words[i].setFlag(Word.F_EL, false);
    }
    sv.nl(a, b);
    return e;
  }
  
  
  private int search(String s, int sp, float w) { // returns maximum length with width<w; TODO optimize somehow
    int bS = sp;
    int bE = s.length()+1;
    while (bS+1<bE) {
      int c = (bS+bE)/2; // TODO do something about surrogate pairs
      if (f.widthf(s.substring(sp, c)) < w) bS = c;
      else bE = c;
    }
    return bS;
  }
  
  
  
  
  public static class Word {
    public static final byte F_LN = 1; // whether word ends with a newline
    public static final byte F_SL = 2; // whether word starts within the first line
    public static final byte F_EL = 4; // whether word ends within the last line
    public byte flags;
    public float x; // position of top-left corner (of first line if split)
    public short y;
    public float w; // total width in pixels
    public final String s; // actual text (only for type 0)
    public String[] split; // if not null, split this word into multiple lines by this; guaranteed at least two items
    public Paragraph overkill; // overkill text rendering
    public Word(String s, int flags) {
      this.s = s;
      this.flags = (byte)flags;
    }
    
    public Paragraph buildPara(StringNode n) {
      return n.buildPara(s);
    }
    
    public void setFont(Font f) {
      w = f.widthf(s);
    }
    public boolean f(byte f) { return (flags&f)!=0; }
    
    public void setFlag(byte f, boolean v) {
      if (v) flags|= f;
      else flags&= ~f;
    }
  }
  
  public Paragraph buildPara(String s) {
    Graphics.tmpParaStyle.setTextStyle(f.textStyle(colFG));
    ParagraphBuilder b = new ParagraphBuilder(Graphics.tmpParaStyle, Typeface.fontCol);
    b.addText(s);
    Paragraph r = b.build();
    r.layout(Float.POSITIVE_INFINITY);
    b.close();
    return r;
  }
  
  public static Word[] words(String s) { // each string will either have no whitespace or be all spaces, or be a single newline
    Vec<Word> v = new Vec<>();
    int pi = 0;
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i++);
      if (c=='\n' | c=='\r') {
        if (c=='\r' && i<s.length() && s.charAt(i)=='\n') i++;
        v.add(new Word(s.substring(pi, i), Word.F_LN)); // TODO either include newline, or add it in in scanSelection
        pi = i;
      } else if (c==' ' || c=='\t') {
        v.add(new Word(s.substring(pi, i), 0));
        pi = i;
      }
    }
    if (pi!=s.length()) v.add(new Word(s.substring(pi), 0));
    return v.toArray(new Word[0]);
  }
  
  public Node findCh(int x, int y) {
    return null;
  }
  
  public void resized() {
    assert !(p instanceof InlineNode);
  }
  
  public Vec<Prop> getProps() {
    return addProps(super.getProps(), p instanceof InlineNode || hasProp("color")? "str.color" : null);
  }
}
