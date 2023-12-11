package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.utils.Scroller;
import dzaima.utils.*;

public class ScrollNode extends FrameNode implements Scroller.Scrollable {
  private int chW, chH;
  boolean hVis, vVis, hOpen, vOpen, tempOverlap; // TODO not do temp thing
  
  private int barCol, thumbCol, barSize, xMode, yMode;
  public ScrollNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    aTick();
  }
  
  public boolean scrollStop() { return !visible; }
  public XY scrollCurr() {
    Node c=ch();
    return new XY(-c.dx, -c.dy);
  }
  public XY scrollMax() {
    isz();
    return new XY(hOpen? chW-iw : 0, vOpen? chH-ih : 0);
  }
  public void scrollSetCurr(int x, int y) {
    Node c = ch();
    c.dx = -x;
    c.dy = -y;
    mRedraw();
  }
  public XY scrollTarget() { return Scroller.getTarget(this); }
  
  private static class NodeScroll {
    private final Node n;
    private final Mode stX, stY;
    private final int offX, offY;
    NodeScroll(Node node, Mode stX, int offX, Mode stY, int offY) {
      n = node;
      this.stX = stX; this.offX = offX;
      this.stY = stY; this.offY = offY;
    }
  }
  private static final ExternalField<ScrollNode, Vec<NodeScroll>> FLD_NODE_SCROLL = new ExternalField<>("ScrollNode FLD_NODE_SCROLL");
  public enum Mode { NONE, PARTLY_OFFSCREEN, FULLY_OFFSCREEN, SMOOTH, INSTANT } // TODO split into two, to allow smooth & instant variations of only-on-offscreen
  public static void scrollTo(Node n, Mode x, Mode y) {
    scrollTo(n, x, y, 0, 0);
  }
  public static ScrollNode scrollTo(Node n, Mode x, Mode y, int offX, int offY) {
    ScrollNode sc = nearestScrollNode(n);
    if (sc==null) return null;
    FLD_NODE_SCROLL.getOrSet(sc, Vec::of).add(new NodeScroll(n, x, offX, y, offY));
    sc.ignoreYS();
    sc.ignoreYE();
    return sc;
  }
  public static ScrollNode nearestScrollNode(Node n) {
    Node p = n;
    while (!(p instanceof ScrollNode) && p!=null) p = p.p;
    if (p==null) return null;
    return (ScrollNode) p;
  }
  
  
  public void propsUpd() { super.propsUpd();
    barCol = gc.col(this, "barCol", "scroll.barCol");
    thumbCol = gc.col(this, "thumbCol", "scroll.thumbCol");
    barSize = gc.len(this, "barSize", "scroll.barSize");
    xMode = mode(gc.valD(this, "x", "auto"));
    yMode = mode(gc.valD(this, "y", "auto"));
    tempOverlap = gc.boolD(this, "tempOverlap", false);
  }
  
  public Node ch() { return ch.get(0); }
  
  
  
  public boolean atYS(int err) {
    return scrollTarget().y < err;
  }
  public boolean atYE(int err) {
    return scrollTarget().y+err >= chH-ih;
  }
  int distYE() {
    return chH-ih - scrollTarget().y;
  }
  
  
  private static final ExternalField<ScrollNode, Boolean> FLD_XE = new ExternalField<>("ScrollNode FLD_XE");
  private static final ExternalField<ScrollNode, Boolean> FLD_YE = new ExternalField<>("ScrollNode FLD_YE");
  public void toXS(boolean instant) { FLD_XE.clear(this); Scroller.targetSetX(this, 0, instant); ignoreFocus(); }
  public void toYS(boolean instant) { FLD_YE.clear(this); Scroller.targetSetY(this, 0, instant); ignoreFocus(); }
  public void toXE(boolean instant) { FLD_XE.set(this, instant); mRedraw(); }
  public void toYE(boolean instant) { FLD_YE.set(this, instant); mRedraw(); }
  public void toXY0(boolean instant) { toXS(instant); toYS(instant); }
  
  
  
  public int fillW() {
    if (xMode==HIDDEN) return 0;
    if (xMode==OFF) return (yMode==OFF|yMode==HIDDEN?0:barSize) + ch().minW();
    return gc.em*3;
  }
  public int fillH(int w) {
    if (yMode==HIDDEN) return 0;
    if (yMode==OFF) return (xMode==OFF|xMode==HIDDEN?0:barSize) + ch().minH(xMode==OFF? w : Tools.BIG);
    return gc.em*3;
  }
  public Node getBest(int x, int y) {
    Node c = ch();
    while (true) {
      Node n = c.nearestCh(x, y);
      if (n==null || n.w==-1) return c;
      x-= n.dx;
      y-= n.dy;
      c = n;
    }
  }
  
  private static final int OFF=0, AUTO=1, ON=2, HIDDEN=3;
  int mode(String val) {
    if (val.equals("on")) return ON;
    if (val.equals("auto")) return AUTO;
    if (val.equals("off")) return OFF;
    if (val.equals("hidden")) return HIDDEN;
    throw new RuntimeException("Unknown scroll.x/y value "+val);
  }
  
  
  public static boolean cornerVertical = false; // whether the corner of two scrollbars should be occupied by the vertical one. TODO theme?
  int iw, ih; // inner (shown content) width/height
  public void isz() {
    iw = vVis? w-barSize : w; // TODO these should be just w/h for style=over
    ih = hVis? h-barSize : h;
  }
  
  private void processToLast() {
    isz();
    Boolean jx = FLD_XE.getAndClear(this); if (jx!=null) Scroller.targetSetX(this, chW-iw, jx);
    Boolean jy = FLD_YE.getAndClear(this); if (jy!=null) Scroller.targetSetY(this, chH-ih, jy);
  }
  public void drawCh(Graphics g, boolean full) {
    processToLast();
    isz();
    g.push();
    g.clip(0, 0, iw, ih);
    ch().draw(g, full);
    g.pop();
  }
  
  public int clipSY, clipEY, clipSX, clipEX;
  public void drawC(Graphics g) {
    assert ch.sz==1 : "scroll should have only 1 child node";
    Node c = ch();
  
    clipSX = Math.max(g.clip==null? 0   : g.clip.sx,  0);
    clipEX = Math.min(g.clip==null? g.w : g.clip.ex, iw);
    clipSY = Math.max(g.clip==null? 0   : g.clip.sy,  0);
    clipEY = Math.min(g.clip==null? g.h : g.clip.ey, ih);
    
    processToLast();
    isz();
    if (hVis | vVis) {
      if (vVis) {
        int vH = cornerVertical? h : ih;
        g.rect(w-barSize, 0, w, h, barCol);
        if (vOpen) {
          float barProp = ih/(float)chH; // preferred scaling down
          float thH = Math.max(gc.em, barProp*ih); // thumb height
          float y0 = Tools.map(-c.dy, 0, chH-ih, 0, vH-thH); // thumb start; 
          g.rect(w-barSize, Tools.limit(y0, 0, h), w, Tools.limit(y0+thH, 0, h), thumbCol);
        }
      }
      if (hVis) {
        g.rect(0, h-barSize, iw, h, barCol);
        if (hOpen) {
          float barProp = iw/(float)chW;
          float thW = Math.max(gc.em, barProp*iw);
          float x0 = Tools.map(-c.dx, 0, chW-iw, 0, iw-thW);
          g.rect(Tools.limit(x0, 0, w), h-barSize, Tools.limit(x0+thW, 0, w), h, thumbCol);
        }
      }
    }
  }
  
  private static boolean move(Mode m, int os, int oe, int ts, int te) {
    switch (m) { default: throw new IllegalStateException();
      case NONE: return false;
      case SMOOTH: case INSTANT: return true;
      case PARTLY_OFFSCREEN: return os<ts || oe>te;
      case FULLY_OFFSCREEN: return os>te || oe<ts;
    }
  }
  public void tickC() {
    Scroller.tick(this);
    if ((flags&RS_CH)==0) evalScrollTo();
  }
  private void evalScrollTo() {
    for (NodeScroll e : FLD_NODE_SCROLL.getAndClearOrDefault(this, Vec::of)) {
      Node c = ch();
      Node n = e.n;
      XY rel = c==n? XY.ZERO : n.relPos(c);
      int rx = rel.x+e.offX;
      int ry = rel.y+e.offY;
      
      XY t = scrollTarget();
      if (move(e.stX, rx, rx+n.w, clipSX+t.x, clipEX+t.x)) Scroller.targetSetX(this, rx - (clipSX+clipEX)/2, e.stX==Mode.INSTANT);
      if (move(e.stY, ry, ry+n.h, clipSY+t.y, clipEY+t.y)) Scroller.targetSetY(this, ry - (clipSY+clipEY)/2, e.stY==Mode.INSTANT);
      
      mRedraw();
    }
  }
  
  public Node findCh(int x, int y) { return ch(); }
  
  public boolean scroll(int x, int y, float dx, float dy) {
    if (super.scroll(x, y, dx, dy)) return true;
    if (!hOpen && !vOpen) return false;
    if (dx==0 && yMode==OFF) dx = dy;
    if (!vOpen && dx==0) return false;
    if (!hOpen && dy==0) return false;
    Scroller.scrollInput(this, dx, dy);
    return true;
  }
  
  int selBar = 0; // 1 - x; 2 - y; 3 - xy drag
  
  public void mouseStart(int x, int y, Click c) {
    if (c.bL()) {
      boolean yS = y > h-barSize;
      boolean xS = x > w-barSize;
      if (yS && xS) selBar = cornerVertical || !hVis? 2 : 0;
      else if (vVis && xS) selBar = 2;
      else if (hVis && yS) selBar = 1;
      else selBar = gc.dragScroll? 3 : 0;
      if (selBar!=0) {
        c.register(this, x, y);
        return;
      }
    }
    super.mouseStart(x, y, c);
  }
  
  public void mouseTick(int x, int y, Click cl) {
    int dx = cl.dx;
    int dy = cl.dy;
    if (selBar==2) {
      int vH = cornerVertical? h : ih;
      float barProp = ih/(float)chH; // modified copy-paste from drawC
      float thH = Math.max(gc.em, barProp*ih);
      float d = dy * (chH-ih) / (vH-thH);
      Scroller.deltaDrag(this, 0, (int) d);
    } else if (selBar==1) {
      float barProp = iw/(float)chW;
      float thW = Math.max(gc.em, barProp*iw);
      float d = dx * (chW-iw) / (iw-thW);
      Scroller.deltaDrag(this, (int) d, 0);
    } else { // TODO fancy bounce-back around edges, velocity decay upon release
      assert selBar==3;
      Scroller.deltaDrag(this, -dx, -dy);
    }
    mRedraw();
  }
  
  public boolean key(int x, int y, Key key, int scancode, KeyAction a) {
    if (super.key(x, y, key, scancode, a)) return true;
    boolean instant = false;
    switch (gc.keymap(key, a, "scroll")) {
      case "toXYs": toXY0(instant); break;
      case "toXYe": toXE(instant); toYE(instant); break;
      case "toXs": toXS(instant); break;
      case "toYs": toYS(instant); break;
      case "toXe": toYE(instant); break;
      case "toYe": toXE(instant); break;
    }
    return false;
  }
  
  
  
  private boolean ignoreYE, ignoreYS, ignoreFocus;
  // don't attempt to pin the specified location on the next resize
  public void ignoreYS() { ignoreYS = true; }
  public void ignoreYE() { ignoreYE = true; }
  public void ignoreFocus() { ignoreFocus = true; }
  
  public boolean ignoresYS() { return ignoreYS; }
  public boolean ignoresYE() { return ignoreYE; }
  
  public void resized() {
    Node focusEl;
    Vec<NodeScroll> nds = FLD_NODE_SCROLL.get(this); // TODO perhaps shouldn't do this
    if (w==-1 && nds!=null) {
      focusEl = nds.peek().n;
    } else {
      XY t = scrollTarget();
      focusEl = getBest(
        (clipSX+clipEX)/2 + t.x,
        (clipSY+clipEY)/2 + t.y
      );
    }
    if (focusEl==this) focusEl = null;
    XY fS = focusEl==null? XY.ZERO : focusEl.relPos(this);
    
    boolean atYS = atYS(2) && !ignoreYS; ignoreYS = false;
    boolean atYE = atYE(2) && !ignoreYE; ignoreYE = false;
    int de = distYE();
    
    Node c = ch();
    int wCov = yMode==OFF | yMode==HIDDEN | tempOverlap? 0 : barSize;
    int hCov = xMode==OFF | xMode==HIDDEN | tempOverlap? 0 : barSize;
    int subW = w - wCov;
    int subH = h - hCov;
    chW = xMode==OFF? subW : Math.max(Math.min(c.maxW(   ), subW), c.minW(   ));
    chH = yMode==OFF? subH : Math.max(Math.min(c.maxH(chW), subH), c.minH(chW));
    vOpen = yMode!=OFF && chH>h;                vVis = (yMode==ON | vOpen) & yMode!=HIDDEN;
    hOpen = xMode!=OFF && chW>(vVis? subW : w); hVis = (xMode==ON | hOpen) & xMode!=HIDDEN;
    vOpen = yMode!=OFF && chH>(hVis? subH : h); vVis = (yMode==ON | vOpen) & yMode!=HIDDEN;
    c.resize(chW, chH, c.dx, c.dy);
    mRedraw();
    
    XY fE = focusEl==null? XY.ZERO : focusEl.relPos(this);
    isz();
    if (atYS) {
      // do nothing
    } else if (atYE) {
      Scroller.deltaTranslate(this, 0, distYE()-de);
    } else if (!ignoreFocus) {
      Scroller.deltaTranslate(this, fE.x-fS.x, fE.y-fS.y);
    }
    evalScrollTo();
    Scroller.resized(this);
    ignoreFocus = false;
  }
}