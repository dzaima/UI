package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public class ScrollNode extends FrameNode {
  public int ox, oy; // target offset
  private int chW, chH;
  boolean hVis, vVis, hOpen, vOpen, tempOverlap; // TODO not do temp thing
  
  private int barCol, thumbCol, barSize, xMode, yMode;
  public ScrollNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    aTick();
  }
  
  private Node scrollTo;
  public static void scrollTo(Node n) {
    Node p = n;
    while (!(p instanceof ScrollNode) && p!=null) p = p.p;
    if (p==null) return;
    ScrollNode sc = (ScrollNode) p;
    sc.scrollTo = n;
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
  
  
  public void quiet(int x, int y) {
    Node ch = ch();
    ox+= x; ch.dx+= x;
    oy+= y; ch.dy+= y;
    mRedraw();
  }
  public void loud(int x, int y) {
    ox+= x;
    oy+= y;
    mRedraw();
  }
  public int toLastState = 0; // 0-none; 1-smooth; 2-instant
  public void toLast(boolean instant) {
    mRedraw();
    toLastState = Math.max(toLastState, instant?2:1);
  }
  public boolean atEnd(int err) {
    return oy-err<=ih-chH;
  }
  public void toFirst(boolean quiet) {
    ox = oy = 0;
    if (quiet) ch().dy = ch().dx = 0;
  }
  private boolean toRight;
  public void toRight() { mRedraw(); toRight = true; }
  
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
    Node c = this; x+= dx; y+= dy;
    while (true) {
      x-= c.dx;
      y-= c.dy;
      Node n = c.nearestCh(x, y);
      if (n==null) return c;
      c = n;
    }
  }
  public void resized() {
    Node focusEl = getBest(0, (clipSY+clipEY)/2);
    XY fS = focusEl.relPos(this);
    boolean atEnd = atEnd(5);
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
    limit();
    XY fE = focusEl.relPos(this);
    if (ctx.win().windowResize) {
      if (atEnd) {
        toLast(true);
      } else {
        quiet(fS.x-fE.x, fS.y-fE.y);
        limit();
      }
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
  int iw, ih; // inner width/height
  public void isz() {
    iw = vVis? w-barSize : w; // TODO these should be just w/h for style=over
    ih = hVis? h-barSize : h;
  }
  
  public void drawCh(Graphics g, boolean full) {
    g.push();
    g.clip(0, 0, iw, ih);
    ch().draw(g, full);
    g.pop();
  }
  
  private int clipSY, clipEY;
  public void drawC(Graphics g) {
    clipSY = Math.max(g.clip==null? 0   : g.clip.sy,  0);
    clipEY = Math.min(g.clip==null? g.h : g.clip.ey, ih);
    assert ch.sz==1 : "scroll should have only 1 child node";
    Node c = ch();
    isz();
    if (toLastState!=0) {
      oy = ih-chH;
      ox = 0;
      limit();
      if (toLastState==2) {
        c.dy = oy;
        c.dx = 0;
      }
      toLastState = 0;
    }
    if (toRight) {
      ox = iw-chW;
      limit();
      c.dx = ox;
      toRight = false;
    }
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
  
  private long lastNs;
  public void tickC() {
    Node c = ch();
    if (scrollTo!=null) {
      XY rel = scrollTo.relPos(this);
      if (rel.y+scrollTo.h<0 || rel.y>h) {
        loud(-rel.x, (clipEY-clipSY)/2 - rel.y);
        limit();
      }
      scrollTo = null;
    }
    if (ox!=c.dx | oy!=c.dy) {
      float speed = (float) Math.pow(gc.getProp("scroll.smooth").f(), (gc.lastNs-lastNs) * 60e-9);
      int ndx = (int) (c.dx*speed + ox*(1-speed)); c.dx = ndx==c.dx? ox : ndx;
      int ndy = (int) (c.dy*speed + oy*(1-speed)); c.dy = ndy==c.dy? oy : ndy;
      mRedraw();
    }
    lastNs = gc.lastNs;
  }
  
  private void limit() {
    isz();
    int pox = ox; ox = -Math.max(0, Math.min(-ox, chW-iw)); if (!hOpen) ox = 0;
    int poy = oy; oy = -Math.max(0, Math.min(-oy, chH-ih)); if (!vOpen) oy = 0;
    if (pox!=ox || poy!=oy) mRedraw();
  }
  
  public Node findCh(int x, int y) { return ch(); }
  
  public boolean scroll(int x, int y, float dx, float dy) {
    if (super.scroll(x, y, dx, dy)) return true;
    if (dx==0 && yMode==OFF) dx=dy;
    if (!vOpen && dx==0) return false;
    if (!hOpen && dy==0) return false;
    int sz = gc.em*7; // TODO theme option
    ox+= dx*sz;
    oy+= dy*sz;
    limit();
    return true;
  }
  
  int selBar = 0; // 1 - x; 2 - y; 3 - xy drag
  public boolean mouseDown(int x, int y, Click c) {
    if (c.btn!=Click.LEFT) return super.mouseDown(x, y, c);
    boolean yS = y > h-barSize;
    boolean xS = x > w-barSize;
    if (yS && xS) selBar = cornerVertical || !hVis? 2 : 0;
    else if (vVis && xS) selBar = 2;
    else if (hVis && yS) selBar = 1;
    else selBar = gc.dragScroll? 3 : 0;
    if (selBar!=0) {
      c.notify(this, x, y);
      return true;
    }
    return super.mouseDown(x, y, c);
  }
  
  public void mouseTick(int x, int y, Click cl) {
    int dx = cl.cdx;
    int dy = cl.cdy;
    Node c = ch();
    if (selBar==2) {
      int vH = cornerVertical? h : ih;
      float barProp = ih/(float)chH; // modified copy-paste from drawC
      float thH = Math.max(gc.em, barProp*ih);
      float d = dy * (chH-ih) / (vH-thH);
      oy-= d;
      limit();
      c.dy = oy;
    } else if (selBar==1) {
      float barProp = iw/(float)chW;
      float thW = Math.max(gc.em, barProp*iw);
      ox-= dx * (chW-iw) / (iw-thW);
      limit();
      c.dx = ox;
    } else { // TODO fancy sliding
      assert selBar==3;
      ox+= dx;
      oy+= dy;
      limit();
      c.dx = ox;
      c.dy = oy;
    }
    mRedraw();
  }
  
  public void mouseUp(int x, int y, Click cl) {
    if (gc.isClick(cl)) {
      if (super.mouseDown(cl.lx, cl.ly, cl)) cl.stop();
    }
  }
  
  public boolean key(int x, int y, Key key, int scancode, KeyAction a) {
    if (super.key(x, y, key, scancode, a)) return true;
    if (a.release) return false;
    if (key.k_home()) {
      toFirst(false);
      return true;
    }
    if (key.k_end()) {
      toLast(false);
      return true;
    }
    return false;
  }
}