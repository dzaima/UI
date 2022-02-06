package dzaima.ui.node.types.editable;

public abstract class Pointer {
  public abstract void dx(int x, int y, int dx);
  public abstract void ln(int x, int y);
  public abstract void collapse(int x0, int y0, int x1, int y1);
  
  
  
  public static boolean in(int x, int y, int x0, int y0, int x1, int y1) { // incl both ends
    if (y<y0 | y>y1) return false;
    return (y!=y0 | x>=x0) & (y!=y1 | x<=x1);
  }
  
  public static boolean from(int x, int y, int xc, int yc) { // (x;y)>=(xc;yc)
    return y>yc  ||  y==yc && x>=xc;
  }
  public static boolean to(int x, int y, int xc, int yc) { // (x;y)<=(xc;yc)
    return y<yc  ||  y==yc && x<=xc;
  }
  public static boolean before(int x, int y, int xc, int yc) { // (x;y)<(xc;yc)
    return y<yc  ||  y==yc && x<xc;
  }
  public static boolean after(int x, int y, int xc, int yc) { // (x;y)>(xc;yc)
    return y>yc  ||  y==yc && x>xc;
  }
}
