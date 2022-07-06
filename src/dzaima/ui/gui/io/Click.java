package dzaima.ui.gui.io;

import dzaima.ui.gui.Window;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.utils.*;

public class Click {
  public final Window w;
  public final int btn;
  public static final int LEFT    = 0;
  public static final int RIGHT   = 1;
  public static final int CENTER  = 2;
  public static final int BACK    = 3;
  public static final int FORWARD = 4;
  
  public boolean down;
  
  public int mod0; // modifiers as per Key.M_* on the starting frame
  public int sx, sy; // global x/y start
  public int cx, cy; // global x/y current (or last position, if released)
  public int dx, dy; // global x/y delta from previous frame
  public long prevMs; // previous click start ms
  public long startMs; // current click start ms; will be set to 0 in onDoubleClick
  
  public Click(Window w, int btn) {
    this.w = w;
    this.btn = btn;
  }
  
  public boolean bL() { return btn==LEFT; }
  public boolean bC() { return btn==CENTER; }
  public boolean bR() { return btn==RIGHT; }
  public boolean bB() { return btn==BACK; }
  public boolean bF() { return btn==FORWARD; }
  
  public float len() {
    int dx = sx - cx;
    int dy = sy - cy;
    return (float) Math.sqrt(dx*dx + dy*dy);
  }
  
  
  int state = 0; // 0:none; 1:in-progress; 2:ending
  
  Vec<Request> queue = new Vec<>();
  int pos = -2;
  public Request current() {
    if (pos>=queue.sz) return null;
    return queue.get(pos);
  }
  public boolean queueAtEnd() {
    return pos==queue.sz;
  }
  public boolean queueWasEmpty() {
    return queue.sz==0;
  }
  
  public void startClick() {
    state = 1;
    queue.sz0();
    pos = -1;
    prevMs = startMs;
    startMs = System.currentTimeMillis();
    cx = cy = sx = sy = dx = dy = Integer.MIN_VALUE;
  }
  public boolean nextItem() {
    pos++;
    Request r = current();
    if (r==null) return false;
    r.n.mouseDown(r.x, r.y, this);
    return true;
  }
  public void initialTick(int gx0, int gy0) {
    dx = dy = 0;
    cx = sx = gx0;
    cy = sy = gy0;
  }
  public boolean tickClick(int ngx, int ngy) {
    if (state>0) {
      dx = ngx-cx;
      dy = ngy-cy;
      cx = ngx;
      cy = ngy;
      tickClick();
      return true;
    }
    return false;
  }
  void tickClick() {
    Request r = current();
    try {
      if (r==null) return;
      XY np = r.n.relPos(null);
      r.n.mouseTick(cx-np.x, cy-np.y, this);
    } catch (Throwable t) {
      onClickError(t, r);
    }
  }
  public void endClick() {
    if (state!=1) return; // in case window didn't call corresponding mouseDown
    state = 2;
    Request r = current();
    try {
      if (r!=null) {
        XY np = r.n.relPos(null);
        int x = cx - np.x;
        int y = cy - np.y;
        r.n.mouseTick(x, y, this);
        r = current(); // reloading current as mouseTick may cause it to end
        if (r!=null) r.n.mouseUp(x, y, this);
      }
    } catch (Throwable t) {
      onClickError(t, r);
    }
    state = 0;
  }
  private void onClickError(Throwable t, Request r) {
    Log.error("click", "Error while executing click event:");
    Window.onError(w, t, "click", r.n instanceof Node? (Node) r.n : null);
  }
  
  
  
  public void register(RequestImpl n, int x, int y) {
    queue.add(new Request(n, x, y));
  }
  public void unregister() {
    if (nextItem()) {
      assert state==1 || state==2;
      if (state==1) tickClick();
      else endClick();
    }
  }
  public void replace(RequestImpl n, int x, int y) {
    assert pos < queue.sz;
    queue.set(pos, new Request(n, x, y));
  }
  public boolean onClickEnd() {
    if (!current().n.gc().isClick(this)) {
      unregister();
      return true;
    }
    return false;
  }
  public boolean onDoubleClick() {
    if (current().n.gc().isDoubleclick(this)) {
      startMs = 0;
      return true;
    }
    return false;
  }
  
  
  
  public static class Request {
    public final RequestImpl n;
    public final int x, y;
    
    public Request(RequestImpl n, int x, int y) {
      this.n = n;
      this.x = x;
      this.y = y;
    }
  }
  public interface RequestImpl {
    void mouseDown(int x, int y, Click c);
    void mouseTick(int x, int y, Click c);
    void mouseUp(int x, int y, Click c);
    GConfig gc();
    XY relPos(Node nullArgument);
  }
}
