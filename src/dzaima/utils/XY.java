package dzaima.utils;

public final class XY {
  public static final XY ZERO = new XY(0, 0);
  public static final XY INF = new XY(Tools.BIG, Tools.BIG);
  public final int x, y;
  public XY(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  public static boolean inWH(int x, int y, int x0, int y0, int w, int h) {
    return y>=y0 & y<y0+h
        && x>=x0 & x<x0+w;
  }
  public static boolean in(int x, int y, int x0, int y0, int x1, int y1) {
    return y>=y0 & y<y1
        && x>=x0 & x<x1;
  }
  public static boolean inL(int v, int s, int l) { return v>=s & v<s+l; }
  
  public boolean le(XY that) {
    return this.x<=that.x & this.y<=that.y;
  }
  
  public XY min(int x2, int y2) { return new XY(Math.min(x, x2), Math.min(y, y2)); }
  public XY max(int x2, int y2) { return new XY(Math.max(x, x2), Math.max(y, y2)); }
  public XY min(XY t) { return min(t.x, t.y); }
  public XY max(XY t) { return max(t.x, t.y); }
  
  public XY add(int dx, int dy) { return new XY(x+dx, y+dy); }
  public XY sub(int dx, int dy) { return new XY(x-dx, y-dy); }
  public XY add(XY o) { return new XY(x+o.x, y+o.y); }
  public XY sub(XY o) { return new XY(x-o.x, y-o.y); }
  
  public static int dist(int x, int y, int x0, int y0, int w, int h) {
    x-= x0; int xd = x<0? -x : x>w? x-w : 0;
    y-= y0; int yd = y<0? -y : y>h? y-h : 0;
    return Math.max(xd, yd);
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof XY)) return false; 
    XY t = (XY) o;
    return x==t.x && y==t.y;
  }
  public int hashCode() { return 31*x + y; }
  
  public String toString() { return "("+x+" "+y+")"; }
}
