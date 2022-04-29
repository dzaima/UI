package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public class ReorderableNode extends FrameNode {
  public ReorderableNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public /*open*/ void reorderStarted(Node n) { }
  public /*open*/ void reorderEnded(int oldIdx, int newIdx, Node n) { }
  public boolean reordering() { return reVW!=null; }
  public boolean holding(Node n) { return reVW!=null && takenNode==n; }
  
  
  protected boolean v, shadow;
  protected byte mode;
  protected int pad;
  public void propsUpd() {
    super.propsUpd();
    switch (vs[id("dir")].val()) { default: throw new RuntimeException("Bad ReorderableNode \"dir\" value "+vs[id("dir")]);
      case "v": v=true; break;
      case "h": v=false; break;
    }
    switch (vs[id("mode")].val()) { default: throw new RuntimeException("Bad ReorderableNode \"mode\" value "+vs[id("mode")]);
      case "none": mode = 0; break;
      case "drag": mode = 1; break;
      case "instant": mode = 2; break;
    }
    shadow = gc.boolD(this, "shadow", true);
    pad = gc.lenD(this, "pad", 0);
  }
  
  // TODO binary search trimming drawCh
  
  public int fillW() {
    return v? Solve.vMinW(ch)
            : Solve.hMinW(ch)+Math.max(0, pad*(ch.sz-1));
  }
  public int fillH(int w) {
    if (!v) w-= Math.max(0, pad*(ch.sz-1));
    return v? Solve.vMinH(ch, w)+Math.max(0, pad*(ch.sz-1))
            : Solve.hMinH(ch, w);
  }
  
  
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    assert currIdx==-1;
    if (mode!=0 && c.bL()) c.register(this, x, y);
  }
  
  int origIdx, currIdx = -1;
  ReVW reVW; // reVW!=null indicates reordering
  Node takenNode;
  int takeX, takeY;
  boolean canceled;
  
  public void mouseDown(int x, int y, Click c) {
    if (!c.bL()) return;
    canceled = false;
    takenNode = findCh(x, y);
    if (takenNode==null) { c.unregister(); return; }
    takeX = x-takenNode.dx;
    takeY = y-takenNode.dy;
  }
  
  public void mouseTick(int x, int y, Click c) {
    if (!c.bL() || canceled) return;
    if (reVW==null && (mode==2 || !gc.isClick(c))) {
      origIdx = currIdx = ch.indexOf(takenNode);
      replace(currIdx, p1 -> new PlaceholderNode(ctx, p1));
      ctx.win().addVW(reVW = new ReVW(takenNode));
      reorderStarted(takenNode);
    }
    
    if (currIdx!=-1) {
      XY pos = relPos(null);
      int nw = takenNode.w;
      int nh = takenNode.h;
      int nx =  v? 0 : Math.max(0, Math.min(x-takeX, w-nw));
      int ny = !v? 0 : Math.max(0, Math.min(y-takeY, h-nh));
      reVW.setPos(pos.x+nx, pos.y+ny, nw, nh);
      int ci = currIdx;
      while (ci+1 < ch.sz) {
        Node n = ch.get(ci+1);
        if (v? ny+nh <= n.dy+n.h/2
             : nx+nw <= n.dx+n.w/2) break;
        swap(ci, ci+1);
        ci++;
      }
      while (ci!=0) {
        Node n = ch.get(ci-1);
        if (v? ny > n.dy+n.h/2
             : nx > n.dx+n.w/2) break;
        swap(ci, ci-1);
        ci--;
      }
      currIdx = ci;
    }
  }
  public void mouseUp(int x, int y, Click c) {
    if (!c.bL()) return;
    if (reVW!=null) stopReorder(true);
    else { c.unregister(); resetVars(); }
  }
  
  public void stopReorder(boolean keep) {
    if (reVW==null) return;
    
    Node p = getPlaceholder();
    if (p==null) return;
    
    if (!keep) {
      remove(currIdx, currIdx+1);
      insert(origIdx, takenNode);
    } else if (origIdx>=ch.sz) {
      cancelInAnyWay();
      return;
    } else {
      replace(currIdx, takenNode);
    }
    reVW = null;
    reorderEnded(origIdx, currIdx, takenNode);
    resetVars();
  }
  
  private void cancelInAnyWay() {
    Log.error("ReorderableNode", "Children list modified during reordering");
    int pi = -1;
    for (int i = 0; i < ch.size(); i++) if (ch.get(i) instanceof PlaceholderNode) { pi = i; break; }
    if (pi!=-1) {
      assert ((PlaceholderNode) ch.get(pi)).n == takenNode; // this not being true is just too broken
      ch.remove(pi, pi+1);
      // insert back only if placeholder was still present; not guaranteed to be correct behavior, but that's somewhat expected in a function explicitly handling already broken code
      if (ch.indexOf(takenNode)==-1) { // make sure it's not double-inserted
        ch.insert(pi, takenNode);
        int oi = origIdx;
        reVW = null;
        reorderEnded(oi, pi, takenNode); // still give the message
      }
    }
    resetVars();
  }
  private void resetVars() {
    takenNode = null;
    reVW = null;
    origIdx = currIdx = -1;
    canceled = true;
  }
  private Node getPlaceholder() {
    Node p = currIdx>=0 && currIdx<ch.sz? ch.get(currIdx) : null;
    
    if (!(p instanceof PlaceholderNode)) { // uh oh something's gone pretty wrong
      cancelInAnyWay();
      return null;
    }
    
    return p;
  }
  
  
  
  protected void resized() {
    boolean r = pad!=0;
    if (v) {
      int y = 0;
      for (Node c : ch) {
        int cw = Math.min(c.maxW(), w);
        int ch = c.minH(cw);
        c.resize(cw, ch, 0, y);
        r|= cw!=w;
        y+= ch+pad;
      }
      r|= y!=h; // TODO can this (& below) even happen?
    } else {
      int x = 0;
      for (Node c : ch) {
        int cw = c.minW();
        int ch = c.minH(cw);
        c.resize(cw, ch, x, 0);
        r|= ch!=h;
        x+= cw+pad;
      }
      r|= x!=w;
    }
    if (r) mRedraw();
  }
  
  
  public static class PlaceholderNode extends Node {
    public final Node n;
    
    public PlaceholderNode(Ctx ctx, Node n) {
      super(ctx, KS_NONE, VS_NONE);
      this.n = n;
      add(n);
    }
    public void drawCh(Graphics g, boolean full) { }
    
    public int minW() { return n.minW(); }
    public int maxW() { return n.maxW(); }
    public int minH(int w) { return n.minH(w); }
    public int maxH(int w) { return n.maxH(w); }
    
    protected void resized() { n.resize(w, h, 0, 0); }
  }
  
  
  
  private class ReVW extends VirtualWindow {
    private final Node nd;
    public ReVW(Node nd) {
      super(ReorderableNode.this.ctx.win());
      this.nd = nd;
    }
    
    public boolean drawShadow() { return true; } // TODO not
    private int lx, ly, lw, lh;
    public void setPos(int x, int y, int w, int h) {
      this.lx = x;
      this.ly = y;
      this.lw = w;
      this.lh = h;
      newRect();
      nd.mRedraw();
    }
    protected Rect getLocation(int pw, int ph) { return Rect.xywh(lx, ly, lw, lh); }
    protected void implDraw(Graphics g, boolean full) { nd.draw(g, full); }
    protected boolean implRequiresRedraw() { return nd.needsRedraw(); }
    public boolean shouldRemove() { return currIdx==-1; }
    public Window.CursorType cursorType() { return Window.CursorType.HAND; }
    
    public boolean fullyOpaque() { return false; }
    public boolean ownsXY(int x, int y) { return true; }
    public void mouseStart(Click cl) { }
    public void initialMouseTick(Click c) { }
    public void scroll(float dx, float dy) { }
    public boolean key(Key key, int scancode, KeyAction a) { return false; }
    public void typed(int p) { }
    public void started() { }
    public void newSize() { }
    public void maybeResize() { }
    public void eventTick() { }
    public void tick() { }
  }
}
