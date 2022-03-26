package dzaima.utils;

import io.github.humbleui.types.IRect;

public class Rect {
  public final int sx, sy, ex, ey;
  
  public Rect(int sx, int sy, int ex, int ey) {
    this.sx = sx;
    this.sy = sy;
    this.ex = ex;
    this.ey = ey;
  }
  
  public Rect(IRect r) {
    sx = r.getLeft();
    sy = r.getTop();
    ex = r.getRight();
    ey = r.getBottom();
  }
  
  public static Rect xywh(int x, int y, int w, int h) {
    return new Rect(x, y, x+w, y+h);
  }
  
  public int w() {
    return ex-sx;
  }
  
  public int h() {
    return ey-sy;
  }
  public boolean intersects(Rect that) {
    return ex <= that.sx || that.ex <= sx || ey <= that.ex || that.ey <= ex;
  }
  public Rect and(Rect that) {
    return new Rect(Math.max(sx, that.sx), Math.max(sy, that.sy), Math.min(ex, that.ex), Math.min(ey, that.ey));
  }
  
  public io.github.humbleui.types.Rect skiaf() {
    return new io.github.humbleui.types.Rect(sx, sy, ex, ey);
  }
  
  public Rect plus(int dx, int dy) {
    return new Rect(sx+dx, sy+dy, ex+dx, ey+dy);
  }
  
  public Rect minus(int dx, int dy) {
    return new Rect(sx-dx, sy-dy, ex-dx, ey-dy);
  }
  
  public Rect centered(int w, int h) {
    int cx = (sx+ex)/2;
    int cy = (sy+ey)/2;
    int hw = w/2;
    int hh = h/2;
    return new Rect(cx-hw, cy-hh, cx+(w-hw), cy+(h-hh));
  }
  
  public String toString() {
    return "r("+sx+";"+sy+"→"+ex+";"+ey+")";
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof Rect)) return false;
    Rect r = (Rect) o;
    return sx==r.sx && sy==r.sy && ex==r.ex && ey==r.ey;
  }
  
  public int hashCode() {
    return 31*(31*(31*sx + sy) + ex) + ey;
  }
}
