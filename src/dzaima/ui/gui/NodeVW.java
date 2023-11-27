package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.utils.*;

public abstract class NodeVW extends VirtualWindow {
  public final Node base;
  public int mx, my;
  public boolean mIn;
  
  public NodeVW(NodeWindow w, GConfig gc, Ctx pctx, PNodeGroup g) {
    super(w);
    Ctx.WindowCtx ctx = new Ctx.WindowCtx(gc, pctx, this);
    this.base = ctx.makeHere(g);
  }
  
  public void started() {
    base.shown();
  }
  
  protected void implDraw(Graphics g, boolean full) {
    base.draw(g, full);
  }
  
  protected boolean implRequiresRedraw() {
    return base.needsRedraw();
  }
  
  public void maybeResize() {
    if (base.needsResize()) newSize();
  }
  public void newSize() {
    long sns = System.nanoTime();
    base.propsTick();
    int nw = Math.max(base.minW(  ), Math.min(rect.w(), base.maxW( )));
    int nh = Math.max(base.minH(nw), Math.min(rect.h(), base.maxH(nw)));
    base.resize(nw, nh, 0, 0);
    long ens = System.nanoTime();
    if (NodeWindow.PRINT_ON_RESIZE && !(w instanceof Devtools)) System.out.println((ens-sns)/1e6d+"ms resize to "+nw+";"+nh);
  }
  
  private final Vec<Click> activeClicks = new Vec<>();
  private Vec<Node> pHover = new Vec<>();
  public void eventTick() {
    mx = w.mx-rect.sx;
    my = w.my-rect.sy;
    activeClicks.filterInplace(c -> c.tickClick(mx, my));
    mIn = w.hoveredVW==this;
    
    Node c = base;
    int cx = mx;
    int cy = my;
    Vec<Node> nHover = new Vec<>();
    int i = 0;
    if (mIn && (w.hijack==null || w.hijack.hDoHover())) while (c!=null) {
      cx-= c.dx;
      cy-= c.dy;
      nHover.add(c);
      Node p = i<pHover.sz? pHover.get(i) : null;
      if (c!=p) {
        if (p!=null) {
          for (int j = i; j < pHover.sz; j++) pHover.get(j).hoverE();
          pHover.sz0();
        }
        c.hoverS();
      }
      c.hoverT(cx, cy);
      
      c = c.findCh(cx, cy);
      i++;
    }
    for (int j = i; j < pHover.sz; j++) pHover.get(j).hoverE();
    pHover = nHover;
  }
  
  private final Vec<Window.CursorType> cursorStack = new Vec<>();
  public short pushCursor(Window.CursorType t) { cursorStack.add(t); return (short) (cursorStack.sz-1); }
  public void popCursor() { cursorStack.pop(); }
  public void replaceCursor(short pos, Window.CursorType t) { cursorStack.set(pos, t); }
  
  private Window.CursorType forcedCursor;
  public void forceCursor(Window.CursorType t) {
    if (forcedCursor!=null) Log.error("forceCursor", "Cursor already forced!");
    forcedCursor = t;
  }
  public void unforceCursor() {
    forcedCursor = null;
  }
  
  public Window.CursorType cursorType() {
    if (forcedCursor!=null) return forcedCursor;
    int i = cursorStack.sz;
    while (--i >= 0) if (cursorStack.get(i) != null) return cursorStack.get(i);
    return Window.CursorType.REGULAR;
  }
  
  public void mouseStart(Click cl) { base.mouseStart(mx, my, cl); }
  public void initialMouseTick(Click c) { c.initialTick(mx, my); activeClicks.add(c); }
  public void scroll(float dx, float dy) { base.scroll(mx, my, dx, dy); }
  public boolean key(Key key, int scancode, KeyAction a) {
    return super.key(key, scancode, a) || base.key(mx, my, key, scancode, a);
  }
  
  public void typed(int p) { base.typed(p); }
  
  public void tick() {
    base.tick();
  }
}
