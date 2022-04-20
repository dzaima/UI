package dzaima.ui.apps.devtools;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class DTGraphNode extends Node {
  Devtools t;
  
  public DTGraphNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  
  private static final RotBuf tmpBuf = new RotBuf();
  static class Item {
    String name;
    RotBuf b;
    int col;
    int p;
    boolean isFr;
    Item(String name) {
      this.name = name;
      isFr = name.equals("frame");
    }
    void s(Devtools t, int sw) {
      b = t.times.get(name);
      if (b==null) b = tmpBuf;
      p = Math.floorMod(b.i - sw + (isFr? 0 : -1), RotBuf.BUF_LEN);
    }
    long next() {
      long v = b.ls[p];
      if (++p >= RotBuf.BUF_LEN) p = 0;
      return v;
    }
  }
  private Item[] items;
  private int lineCol;
  public void propsUpd() { super.propsUpd();
    items = new Item[ITEMS.length];
    for (int i = 0; i < items.length; i++) {
      Item c = items[i] = new Item(ITEMS[i]);
      c.col = gc.getProp("devtools.graph."+c.name+"Col").col();
    }
    lineCol = gc.getProp("devtools.graph.lineCol").col();
  }
  
  private static final String[] ITEMS = {"event", "tick", "draw", "flush", "frame"};
  private final Runtime rt = Runtime.getRuntime();
  public void drawC(Graphics g) {
    int mH = (int) (gc.em*1.1); // memory bar height
    int gH = h-mH; // graph height
    
    g.push();
    g.translate(0, mH);
    g.clip(0, 0, w, gH);
    float sc = gH / (1e9f/20);
    int sw = Math.min(w, RotBuf.BUF_LEN);
    int off = w-sw;
    
    Item[] items = this.items;
    for (Item c : items) c.s(t, sw);
    
    for (int i = 0; i < sw; i++) {
      int cy = gH;
      for (Item c : items) {
        int l = (int) (c.next()*sc);
        int nl = c.isFr? gH-l : cy-l;
        g.line(i+off, nl, i+off, cy, c.col);
        cy = nl;
      }
    }
    int l60 = gH - (int) (1e9/60*sc); g.line(0, l60, w, l60, lineCol);
    int l30 = gH - (int) (1e9/30*sc); g.line(0, l30, w, l30, lineCol);
    g.pop();
    
    
    long tot = rt.totalMemory();
    long used = tot-rt.freeMemory();
    g.push();
    
    float frac = (float)used * w / tot;
    g.rect(0, 0, frac, mH, gc.getProp("devtools.memUsed").col());
    g.rect(frac, 0, w, mH, gc.getProp("devtools.memFree").col());
    
    long mb = 1024*1024;
    Font f = gc.defFont;
    g.text((used/mb)+" / "+(tot/mb)+"M", f, 0, f.ascI, gc.getProp("str.color").col());
    g.pop();
  }
  
  public void mouseStart(int x, int y, Click c) { c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) {
    ctx.focus(this);
    aTick();
  }
  
  public void tickC() {
    if (ctx.focusedNode()==this) mRedraw();
    super.tickC();
  }
}
