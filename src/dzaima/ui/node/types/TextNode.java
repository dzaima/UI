package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public class TextNode extends InlineNode {
  public TextNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void addInline(InlineSolver sv) {
    int pcol = sv.tcol; Font pf = sv.f;
    if (p instanceof InlineNode && sv.resize) {
      dx=dy=0;
      w = sv.w;
    }
    
    sv.f = updFont(sv.f);
    if (hasCol) sv.tcol = col;
    // if (hasBg) sv.tbg = bgCol;
    
    if (sv.x==0) sv.a = sv.b = 0; // ignore height of a previous trailing newline if there are things following it; TODO maybe move to InlineNode?
    if(xpad!=0) sv.x = xpad+(int)sv.x; // else, don't round
    for (Node c : ch) sv.add(c);
    if(xpad!=0) sv.x = xpad+(int)sv.x;
    
    sv.f = pf; sv.tcol = pcol;
  }
  protected void baseline(int asc, int dsc) { }
  
  int tsz = -1;
  int col; boolean hasCol;
  int bgCol; boolean hasBg;
  public int xpad;
  short mode;
  boolean hover;
  Typeface tf;
  public void propsUpd() { super.propsUpd();
    tsz = gc.pxD(this, "tsz", -1);
    int cId = id("color"); hasCol = cId>=0; if (hasCol) col   = vs[cId].col();
    int bId = id("bg");    hasBg  = bId>=0; if (hasBg)  bgCol = vs[bId].col();
    if (id("bgCol")!=-1) System.err.println("warning: using old bgCol property");
    mode = 0;
    if (gc.boolD(this, "italics"  , false)) mode|= Typeface.ITALICS;
    if (gc.boolD(this, "bold"     , false)) mode|= Typeface.BOLD;
    if (gc.boolD(this, "strike"   , false)) mode|= Typeface.STRIKE;
    if (gc.boolD(this, "underline", false)) mode|= Typeface.UNDERLINE;
    hover = gc.boolD(this, "hover", false);
    xpad = gc.lenD(this, "xpad", 0);
    String family = gc.strD(this, "family", null);
    if (family!=null) tf = Typeface.of(family);
  }
  
  public Font getFont() {
    return updFont(super.getFont());
  }
  public Font updFont(Font f) {
    Typeface ntf = tf!=null? tf : f.tf;
    return ntf.sizeMode(tsz!=-1? tsz : f.sz, f.mode|mode);
  }
  
  public void bg(Graphics g, boolean full) {
    if (Tools.st(bgCol)) pbg(g, full);
    if (Tools.vs(bgCol)) {
      if (sY1==eY1) {
        g.rect(sX, sY1, eX, eY2, bgCol);
      } else {
        g.rect(sX, sY1, w, sY2, bgCol);
        if (sY2<eY1) g.rect(0, sY2, w, eY1, bgCol);
        g.rect(0, eY1, eX, eY2, bgCol);
      }
    }
  }
  
  public void drawC(Graphics g) {
    if (xpad>0 && hasBg) {
      g.rect(sX, sY1, sX+xpad, sY2, bgCol);
      g.rect(eX-xpad, eY1, eX, eY2, bgCol);
    }
  }
  
  public int minW() {
    int max = 0;
    for (Node c : ch) max = Math.max(max, c.minW());
    return max + xpad*2;
  }
  public int minH(int w) {
    InlineSolver sv = new InlineSolver(w, gc, false);
    sv.add(this);
    sv.nl();
    return sv.y;
  }
  public int maxW() {
    int w = xpad*2;
    for (Node c : ch) {
      int cw = c.maxW();
      if (cw>=Tools.BIG) return Tools.BIG;
      w+= cw;
    }
    return w;
  }
  
  public void hoverS() { if (hover) ctx.win().setCursor(Window.CursorType.HAND   ); }
  public void hoverE() { if (hover) ctx.win().setCursor(Window.CursorType.REGULAR); }
  
  public void resized() {
    InlineSolver sv = new InlineSolver(w, gc, true);
    sv.add(this);
    sv.nl();
  }
}
