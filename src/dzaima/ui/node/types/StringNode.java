package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
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
    super(ctx, KS_NONE, VS_NONE);
    this.s = s;
  }
  
  
  private String[] lns; // non-inline only; TODO reuse words maybe?
  public Word[] words; // inline only
  private XY sz; // TODO not
  private int maxW;
  
  public void propsUpd() { super.propsUpd();
    if (p instanceof InlineNode) {
      Font cf = ((InlineNode) p).getFont();
      if (words==null) words = words(s);
      maxW = 0;
      int lnW = 0;
      for (Word c : words) {
        if (c.type==2) { maxW=Math.max(maxW,lnW); lnW=0; }
        else lnW+= cf.width(c.s);
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
  public static void text(Graphics g, String s, StringNode n, float x, float y) {
    if (PARAGRAPH_TEXT) g.textP(s, n.f, x, y, n.colFG);
    else g.text(s, n.f, x, y, n.colFG);
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
        if (c.type==0) {
          if (c.split == null) {
            text(g, c, this, c.x, c.y+c.bl);
          } else {
            int y = c.y;
            int i = 0;
            text(g, c.split[i++], this, c.x, y);
            while (i < c.split.length-1) {
              y+= fh;
              text(g, c.split[i++], this, 0, y);
            }
            y+= fh + c.bl - f.ascI;
            text(g, c.split[i], this, 0, y);
          }
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
    XY sel = ctx.win().selectionRange(this);
    int ss = sel==null? -1 : sel.x;
    int se = sel==null? -1 : sel.y;
    if (words!=null && (Tools.vs(colBG) || sel!=null)) {
      int fa = f.ascI;
      int fh = f.hi;
      if (g.clip!=null) if (g.clip.ey<sY1 || g.clip.sy>sY1+h) return;
      int cs = 0;
      for (Word c : words) {
        if (c.type!=0) continue;
        int x = (int) c.x;
        int y = c.y;
        if (c.split == null) {
          cs = bgRect(g, ss, se, cs, x, y+c.bl-fa, c.s, (int) Math.ceil(c.w), fh);
        } else {
          y-= fa;
          int i = 0;
          cs = bgRect(g, ss, se, cs, x, y, c.split[i], f.width(c.split[i]), fh); i++;
          while (i < c.split.length-1) {
            y+= fh;
            cs = bgRect(g, ss, se, cs, 0, y, c.split[i], f.width(c.split[i]), fh); i++;
          }
          y+= fh + c.bl - fa;
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
  
  public int minW(     ) { return sz.x; }
  public int maxW(     ) { return maxW; }
  public int minH(int w) { return sz.y; }
  
  public void addInline(InlineSolver sv) { // TODO this probably deserves to be optimized a lot
    boolean mut = sv.resize;
    if (sv.f != f) {
      f = sv.f;
      for (Word c : words) c.setFont(f);
    }
    if (words.length==0) return;
    
    int a = f.ascI;
    int b = f.dscI;
    lnS = 0;
    
    for (int i = 0; i < words.length; i++) {
      Word c = words[i];
      if (mut) c.split = null;
      if (sv.x+c.w >= sv.w) {
        if (c.w >= sv.w  &&  c.s.length()>1) { // split word into chars; c.type==0 guaranteed by length check
          int x0 = search(c.s, 0, sv.w-sv.x);
          Vec<String> spl = new Vec<>();
          spl.add(c.s.substring(0, x0));
          while (x0 != c.s.length()) {
            int x1 = Math.max(search(c.s, x0, sv.w), x0+1);
            spl.add(c.s.substring(x0, x1));
            x0 = x1;
          }
          sv.ab(a, b);
          if (mut) {
            c.split = spl.toArray(new String[0]);
            c.x = sv.x;
            c.y = sv.y + sv.a;
          }
          baseline(lnS, i, sv.a); lnS = i; sv.nl(); sv.a = a; sv.b = b;
          sv.x = f.width(spl.peek());
          sv.y+= f.hi*(spl.sz-2);
          continue;
        }
        if (i!=0) sv.ab(a, b);
        baseline(lnS, i, sv.a); lnS = i; sv.nl(); sv.a = a; sv.b = b;
        if (c.type==2) { if(mut)c.x=-1; continue; }
      }
      if (mut) {
        c.x = sv.x;
        c.y = sv.y;
      }
      sv.x+= c.w;
    }
    sv.ab(a, b);
    
    colFG = sv.tcol;
    colBG = sv.tbg;
    if (mut) mRedraw();
  }
  
  int lnS;
  protected void baseline(int asc, int dsc) { baseline(lnS, words.length, asc); }
  private void baseline(int s, int e, int asc) { for (int j = s; j < e; j++) words[j].bl = (short) asc; }
  
  
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
    public float x;
    public int y; // not split: baseline = y+bl; split: l0 baseline == y; lN baseline == y+bl+f.hi*n
    public final byte type; // 0 - text; 2 - newline
    public final String s; // actual text (only for type 0)
    public float w; // width in pixels
    public short bl; // baseline position
    public String[] split; // if not null, split this word into multiple lines by this
    public Paragraph overkill; // overkill text rendering
    public Word(byte type, String s) {
      this.s = s;
      this.type = type;
    }
  
    public Paragraph buildPara(StringNode n) {
      return n.buildPara(s);
    }
  
    public void setFont(Font f) {
      w = type==2? Tools.BIG : f.widthf(s);
    }
  }
  
  public Paragraph buildPara(String s) {
    Graphics.tmpStyle.setTextStyle(f.textStyle(colFG));
    ParagraphBuilder b = new ParagraphBuilder(Graphics.tmpStyle, Typeface.fontCol);
    b.addText(s);
    Paragraph r = b.build();
    r.layout(Float.POSITIVE_INFINITY);
    b.close();
    return r;
  }
  
  public static Word[] words(String s) { // each string will either have no whitespace or be all spaces, or be a single newline
    Vec<Word> v = new Vec<>();
    int pi = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c=='\n' | c=='\r') {
        if (pi!=i) v.add(new Word((byte) 0, s.substring(pi, i)));
        if (c=='\r' && i+1<s.length() && s.charAt(i+1)=='\n') i++;
        v.add(new Word((byte) 2, "\n"));
        pi = i+1;
      } else if (c==' ' || c=='\t') {
        v.add(new Word((byte) 0, s.substring(pi, i+1)));
        pi = i+1;
      }
    }
    if (pi!=s.length()) v.add(new Word((byte) 0, s.substring(pi)));
    return v.toArray(new Word[0]);
  }
  
  public Node findCh(int x, int y) {
    return null;
  }
  
  public void resized() {
    assert !(p instanceof InlineNode);
  }
}
