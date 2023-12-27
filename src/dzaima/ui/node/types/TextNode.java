package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public class TextNode extends InlineNode {
  public TextNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void addInline(InlineSolver sv) {
    Font pf = sv.f;
    int pFG = sv.tcol;
    int pBG = sv.tbg;
    if (p instanceof InlineNode && sv.resize) {
      dx=dy=0;
      w = sv.w;
    }
    
    sv.f = updFont(sv.f);
    if (hasCol) sv.tcol = col;
    if (hasBg) sv.tbg = bgCol;
    
    if (sv.x==0) sv.a = sv.b = 0; // ignore height of a previous trailing newline if there are things following it; TODO maybe move to InlineNode?
    if(xpad!=0) sv.x = xpad+(int)sv.x; // else, don't round
    for (Node c : ch) sv.add(c);
    if(xpad!=0) sv.x = xpad+(int)sv.x;
    
    sv.f = pf;
    sv.tcol = pFG;
    sv.tbg = pBG;
  }
  protected void baseline(int asc, int dsc, int h) { }
  
  int tsz = -1;
  int col; boolean hasCol;
  int bgCol; boolean hasBg;
  public int xpad;
  short mode;
  boolean hover;
  Typeface tf;
  public void propsUpd() { super.propsUpd();
    tsz = gc.pxD(this, "tsz", -1);
    Prop c = getPropN("color"); hasCol = c!=null; if (hasCol) col   = c.col();
    Prop b = getPropN("bg");    hasBg  = b!=null; if (hasBg)  bgCol = b.col();
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
  
  // public void bg(Graphics g, boolean full) {
  //   if (Tools.st(bgCol)) pbg(g, full);
  //   if (Tools.vs(bgCol)) {
  //     if (sY1==eY1) {
  //       g.rect(sX, sY1, eX, eY2, bgCol);
  //     } else {
  //       g.rect(sX, sY1, w, sY2, bgCol);
  //       if (sY2<eY1) g.rect(0, sY2, w, eY1, bgCol);
  //       g.rect(0, eY1, eX, eY2, bgCol);
  //     }
  //   }
  // }
  
  
  public void bg(Graphics g, boolean full) {
    if (xpad>0 && hasBg) {
      g.rect(sX, sY1, sX+xpad, sY2, bgCol);
      g.rect(eX-xpad, eY1, eX, eY2, bgCol);
    }
    super.bg(g, full);
  }
  
  public Node findCh(int x, int y) {
    if (XY.in(x, y, sX, sY1, sX+xpad, sY2)) return ch.get(0);
    // ending padding handled by super.findCh returning last if none found
    return super.findCh(x, y);
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
  
  public void hoverS() { if (hover) ctx.vw().pushCursor(Window.CursorType.HAND); }
  public void hoverE() { if (hover) ctx.vw().popCursor(); }
  
  public void resized() {
    InlineSolver sv = new InlineSolver(w, gc, true);
    sv.add(this);
    sv.nl();
  }
}
