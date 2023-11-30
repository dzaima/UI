package dzaima.ui.node.utils;

import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.prop.PropI;
import dzaima.ui.node.types.ScrollNode;
import dzaima.utils.*;

import java.lang.ref.WeakReference;

public abstract class Scroller {
  public static void scrollInput(Scrollable n, float dx, float dy) { // scrolls per scroll event data
    if (dx==0 && dy==0) return;
    PropI p = n.gc().getProp("scroll.nodeSpeed");
    float sz = -p.f();
    // tiny-TODO - should accumulate pixel-fractional values? perhaps should do that at the global level
    targetDelta(n, (int) (dx*sz), (int) (dy*sz), false);
  }
  
  // TODO don't need clamp? only deltaTranslate should care
  public static void targetDelta(Scrollable n,  int dx, int dy, boolean instant) { targetDelta(n, true, dx, true, dy, instant, true); }
  public static void targetDeltaX(Scrollable n, int dx,         boolean instant) { targetDelta(n, true, dx, false, 0, instant, true); }
  public static void targetDeltaY(Scrollable n,         int dy, boolean instant) { targetDelta(n, false, 0, true, dy, instant, true); }
  public static void targetDelta(Scrollable n, boolean rx, int dx, boolean ry, int dy, boolean instant) { targetDelta(n, rx, dx, ry, dy, instant, true); }
  public static void targetDelta(Scrollable n, boolean rx, int dx, boolean ry, int dy, boolean instant, boolean clamp) {
    XY t = mk(n).getTarget_i(n);
    targetSet(n, rx, t.x+dx, ry, t.y+dy, instant, clamp);
  }
  
  public static void targetSet(Scrollable n,  int x, int y, boolean instant) { targetSet(n, true, x, true, y, instant, true); }
  public static void targetSetX(Scrollable n, int x,        boolean instant) { targetSet(n, true, x, false, 0, instant, true); }
  public static void targetSetY(Scrollable n,        int y, boolean instant) { targetSet(n, false, 0, true, y, instant, true); }
  public static void targetSet(Scrollable n, boolean rx, int x, boolean ry, int y, boolean instant) { targetSet(n, rx, x, ry, y, instant, true); }
  public static void targetSet(Scrollable n, boolean rx, int x, boolean ry, int y, boolean instant, boolean clamp) {
    Scroller s = mk(n);
    if (clamp) {
      XY m = n.scrollMax();
      x = Math.min(x, m.x);
      y = Math.min(y, m.y);
    }
    XY t = s.getTarget_i(n);
    s.setTarget(rx? x : t.x, ry? y : t.y);
    if (instant) s.jumpTarget_i(n, rx, ry);
  }
  
  // translate as from a mouse/scrollbar drag
  public static void deltaDrag(Scrollable n, int dx, int dy) {
    targetDelta(n, dx, dy, true);
  }
  
  // translate everything by the specified delta, preserving any unfinished animation to continue coming in the same way; not clamped
  public static void deltaTranslate(Scrollable n, int dx, int dy) {
    if (dx==0 && dy==0) return;
    mk(n).translate_i(n, dx, dy);
  }
  
  private static Scroller mk(Scrollable n) {
    return FLD.getOrInit(n, ExpDecayScroller::new);
  }
  
  public static XY getTarget(Scrollable n) {
    Scroller s = FLD.get(n);
    if (s==null) return n.scrollCurr();
    return s.getTarget_i(n);
  }
  
  public static void tick(ScrollNode n) {
    Scroller s = FLD.get(n);
    if (s!=null) s.tick();
  }
  
  public final void setTarget(int x, int y) {
    Scrollable n = ref.get();
    if (n==null || n.scrollStop()) { done(); return; }
    setTarget_i(n, x, y);
  }
  
  public final void tick() {
    Scrollable n = ref.get();
    if (n==null || n.scrollStop()) { done(); return; }
    tick_i(n);
  }
  
  public interface Scrollable {
    boolean scrollStop();
    XY scrollCurr();
    XY scrollMax();
    void scrollSetCurr(int x, int y);
    GConfig gc();
  }
  
  private final WeakReference<Scrollable> ref;
  
  protected Scroller(Scrollable n) { this.ref = new WeakReference<>(n); }
  private static final ExternalField<Scrollable, Scroller> FLD = new ExternalField<>("Scroller FLD");
  
  protected void done() { FLD.clear(ref.get()); }
  protected abstract void tick_i(Scrollable n);
  protected abstract XY getTarget_i(Scrollable n);
  protected abstract void setTarget_i(Scrollable n, int x, int y);
  protected abstract void translate_i(Scrollable n, int dx, int dy);
  protected abstract void jumpTarget_i(Scrollable n, boolean x, boolean y);
  
  
  
  
  static class ExpDecayScroller extends Scroller {
    public XY tgt; // always ≥0, but may be >scrollMax
    protected ExpDecayScroller(Scrollable n) {
      super(n);
      tgt = n.scrollCurr();
    }
    
    protected void tick_i(Scrollable n) {
      tgt = tgt.min(n.scrollMax());
      XY c = n.scrollCurr();
      if (c.equals(tgt)) { done(); return; }
      GConfig gc = n.gc();
      float speed = (float) Math.pow(gc.getProp("scroll.smooth").f(), gc.deltaNs*60e-9);
      int nx = (int) (c.x*speed + tgt.x*(1-speed)); nx = nx==c.x? tgt.x : nx;
      int ny = (int) (c.y*speed + tgt.y*(1-speed)); ny = ny==c.y? tgt.y : ny;
      n.scrollSetCurr(nx, ny);
    }
    
    protected XY getTarget_i(Scrollable n) {
      return tgt;
    }
    
    protected void setTarget_i(Scrollable n, int x, int y) {
      tgt = new XY(x, y).max(0, 0);
    }
    
    protected void translate_i(Scrollable n, int dx, int dy) {
      tgt = tgt.add(dx, dy).max(0, 0);
      XY p = n.scrollCurr().add(dx, dy);
      n.scrollSetCurr(p.x, p.y);
    }
    
    protected void jumpTarget_i(Scrollable n, boolean x, boolean y) {
      XY p = n.scrollCurr();
      n.scrollSetCurr(x? tgt.x : p.x, y? tgt.y : p.y);
    }
  }
}
