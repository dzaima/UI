package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public class ReorderableNode extends PackedListNode {
  public ReorderableNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public /*open*/ boolean shouldReorder(int idx, Node n) { return true; }
  public /*open*/ Node reorderSelect(Node selected) { return selected; }
  public /*open*/ void reorderStarted(Node n) { }
  public /*open*/ void reorderSwapped() { }
  public /*open*/ void reorderEnded(int oldIdx, int newIdx, Node n) { }
  public /*open*/ void drawPlaceholder(Graphics g, int w, int h) { }
  public boolean reordering() { return reVW!=null; }
  public boolean holding(Node n) { return reVW!=null && takenNode==n; }
  public Node heldNode() { return takenNode; }
  
  
  protected boolean shadow;
  protected int mode;
  public void propsUpd() {
    super.propsUpd();
    switch (vs[id("mode")].val()) { default: throw new RuntimeException("Bad ReorderableNode \"mode\" value "+vs[id("mode")]);
      case "none": mode = 0; break;
      case "drag": mode = 1; break;
      case "instant": mode = 2; break;
    }
    shadow = gc.boolD(this, "shadow", true);
  }
  
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (reVW!=null) {
      Log.error("ReorderableNode", "Reordering while already reordering");
      cancelInAnyWay();
      return;
    }
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
      if (shouldReorder(ch.indexOf(takenNode), takenNode)) {
        takenNode = reorderSelect(takenNode);
        origIdx = currIdx = ch.indexOf(takenNode);
        replace(currIdx, p1 -> new PlaceholderNode(ctx, p1, this));
        ctx.win().addVW(reVW = new ReVW(takenNode, ctx.win().focusedVW));
        reorderStarted(takenNode);
      } else canceled = true;
    }
    
    if (reVW!=null) {
      XY pos = relPos(null);
      int nw = takenNode.w;
      int nh = takenNode.h;
      int nx =  v? 0 : Math.max(0, Math.min(x-takeX, w-nw));
      int ny = !v? 0 : Math.max(0, Math.min(y-takeY, h-nh));
      reVW.setPos(pos.x+nx, pos.y+ny, nw, nh);
      int ci = currIdx;
      boolean swapped = false;
      while (ci+1 < ch.sz) {
        Node n = ch.get(ci+1);
        if (v? ny+nh <= n.dy+n.h/2
             : nx+nw <= n.dx+n.w/2) break;
        swap(ci, ci+1);
        swapped = true;
        ci++;
      }
      if (!swapped) while (ci!=0) { // only try moving up if moving down failed; otherwise the non-recalculated layout messes up movement
        Node n = ch.get(ci-1);
        if (v? ny > n.dy+n.h/2
             : nx > n.dx+n.w/2) break;
        swap(ci, ci-1);
        swapped = true;
        ci--;
      }
      currIdx = ci;
      if (swapped) reorderSwapped();
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
    int oi = origIdx;
    int ci = currIdx;
    Node nd = takenNode;
    resetVars();
    reorderEnded(oi, ci, nd);
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
  
  
  
  public static class PlaceholderNode extends Node {
    public final Node n;
    public final ReorderableNode r;
    
    public PlaceholderNode(Ctx ctx, Node n, ReorderableNode r) {
      super(ctx, KS_NONE, VS_NONE);
      this.n = n;
      this.r = r;
      add(n);
    }
    public void drawC(Graphics g) { r.drawPlaceholder(g, w, h); }
    public void drawCh(Graphics g, boolean full) { }
    
    public int minW() { return n.minW(); }
    public int maxW() { return n.maxW(); }
    public int minH(int w) { return n.minH(w); }
    public int maxH(int w) { return n.maxH(w); }
    
    protected void resized() { n.resize(w, h, 0, 0); }
  }
  
  
  
  private class ReVW extends VirtualWindow {
    private final Node nd;
    private final VirtualWindow prev;
    public ReVW(Node nd, VirtualWindow prev) {
      super(ReorderableNode.this.ctx.win());
      this.nd = nd;
      this.prev = prev;
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
    public void scroll(float dx, float dy) { prev.scroll(dx, dy); }
    public void typed(int p) { }
    public void started() { }
    public void newSize() { }
    public void maybeResize() { }
    public void eventTick() { }
    public void tick() { }
  }
}
