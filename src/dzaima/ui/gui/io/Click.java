package dzaima.ui.gui.io;

import dzaima.ui.node.Node;

public class Click {
  public int btn;
  public static final int LEFT    = 0;
  public static final int RIGHT   = 1;
  public static final int CENTER  = 2;
  public static final int BACK    = 3;
  public static final int FORWARD = 4;
  
  public boolean down;
  
  public int mod; // modifiers as per Key.M_*
  public int sx, sy; // start x/y
  public int dx, dy; // delta x/y travelled since start
  public int cdx, cdy; // current movement at the time of a callback
  public long msStart; // time of start
  public long msPrev; // time of previous click
  public boolean didDouble;
  
  
  public Click(int btn) {
    this.btn = btn;
  }
  
  boolean startedThisTick = false; 
  public void start(int x, int y) {
    sx = x; dx = 0;
    sy = y; dy = 0;
    msStart = System.currentTimeMillis();
    listen = null;
    didDouble = false;
    // startedThisTick = true; // TODO this hasn't fixed the issue of quick movement at start registering in; fix when rewriting delta computation
  }
  public void stop() {
    Node c = listen; listen = null;
    if (c!=null) c.mouseUp(lx+dx, ly+dy, this);
    msPrev = didDouble? 0 : System.currentTimeMillis();
  }
  public void tick(int x, int y) {
    if (startedThisTick) {
      startedThisTick = false;
    } else {
      dx+= x;
      dy+= y;
    }
    cdx = x;
    cdy = y;
    if (listen!=null) listen.mouseTick(lx+dx, ly+dy, this);
  }
  public int duration() { // how long has this been held in milliseconds
    return (int) (System.currentTimeMillis()-msStart);
  }
  
  
  public boolean bL() { return btn==LEFT; }
  public boolean bC() { return btn==CENTER; }
  public boolean bR() { return btn==RIGHT; }
  public boolean bB() { return btn==BACK; }
  public boolean bF() { return btn==FORWARD; }
  
  private Node prevNotify;
  private Node listen;
  public int lx, ly; // position of listened object
  
  public void notify(Node node, int lx, int ly) {
    assert listen==null;
    listen = node;
    if (node!=prevNotify) msPrev = 0;
    prevNotify = node;
    this.lx = lx;
    this.ly = ly;
  }
  
  public float len() {
    return (float) Math.sqrt(dx*dx + dy*dy);
  }
  
  
  public void clearDoubleclick() {
    didDouble = true;
  }
}
