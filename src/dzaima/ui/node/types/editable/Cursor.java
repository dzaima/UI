package dzaima.ui.node.types.editable;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Key;
import dzaima.ui.gui.undo.Undo;
import dzaima.utils.XY;

public class Cursor extends Pointer implements Comparable<Cursor> {
  public final EditNode e;
  public int sx, sy, ex, ey;
  public int px; // preferred X; set to -1 when not needed anymore (for fancy keeping of X while moving through lines of different lengths)
  
  
  public Cursor(EditNode e) {
    this.e = e;
  }
  
  public Cursor(Cursor copy) {
    e = copy.e; px = copy.px;
    sx = copy.sx; sy = copy.sy;
    ex = copy.ex; ey = copy.ey;
  }
  
  // primitive functions
  public void mv(int nsx, int nsy, int nex, int ney) { e.cursorTime = e.gc.lastMs;
    int psx = sx, psy = sy, pex = ex, pey = ey;
    if (psx!=nsx | psy!=nsy | pex!=nex | pey!=ney) {
      e.u(new Undo() {
        public void redo() { e.cursorTime = e.gc.lastMs;
          e.mRedraw();
          sx = nsx; ex = nex;
          sy = nsy; ey = ney;
        }
        public void undo() { e.cursorTime = e.gc.lastMs;
          e.mRedraw();
          sx = psx; ex = pex;
          sy = psy; ey = pey;
        }
      });
    }
  }
  public boolean reg() { return sx==ex & sy==ey; }
  public boolean sel() { return !reg(); }
  public void dx(int x, int y, int dx) {
    int nsx = sx, nex = ex;
    if (sy==y && sx>=x) nsx+= dx;
    if (ey==y && ex>=x) nex+= dx;
    mv(nsx, sy, nex, ey);
  }
  public void ln(int x, int y) {
    int nsx=sx, nsy=sy, nex=ex, ney=ey;
    if (sy>y) nsy++; else if (sy==y && sx>=x) { nsy++; nsx-= x; }
    if (ey>y) ney++; else if (ey==y && ex>=x) { ney++; nex-= x; }
    mv(nsx, nsy, nex, ney);
  }
  public void collapse(int x0, int y0, int x1, int y1) {
    int nsx=sx, nsy=sy, nex=ex, ney=ey;
    if (from(sx,sy, x0,y0)) { if (before(sx,sy, x1,y1)) { nsx=x0; nsy=y0; } else { nsy-=y1-y0; if (sy==y1) nsx-=x1-x0; } }
    if (from(ex,ey, x0,y0)) { if (before(ex,ey, x1,y1)) { nex=x0; ney=y0; } else { ney-=y1-y0; if (ey==y1) nex-=x1-x0; } }
    mv(nsx, nsy, nex, ney);
  }
  
  
  
  // mutating functions
  public void order() {
    if (sy==ey? ex<sx : ey<sy) mv(ex, ey, sx, sy);
  }
  public void clearSel() { px = -1;
    if (sx!=ex | sy!=ey) {
      order();
      e.remove(sx, sy, ex, ey);
    }
  }
  public void typed(int p) {
    clearSel();
    e.insertU(sx, sy, p, true);
  }
  public void delL(int mod) { px = -1;
    if (reg()) left(mod & Key.M_CTRL | Key.M_SHIFT);
    clearSel();
  }
  public void delR(int mod) { px = -1;
    if (reg()) right(mod & Key.M_CTRL | Key.M_SHIFT);
    clearSel();
  }
  
  // movement functions
  public void mv(boolean sh, int x, int y) { mvU(sh, fix(x, e.ln(y)), y); }
  public void mv(int x, int y) { mvU(fix(x, e.ln(y)), y); }
  
  protected void mvU(boolean sh, int x, int y) { mv(sh?sx:x, sh?sy:y, x, y); }
  protected void mvU(int x, int y) { mv(x, y, x, y); }
  
  public void left(int mod) { px=-1;
    if (!Key.shift(mod) && sel()) { order(); mvU(sx, sy); return; }
    int nx=ex, ny=ey;
    if (nx==0) {
      if (ny!=0) {
        ny--;
        nx = e.ln(ny).sz();
      }
    } else {
      EditNode.Line l = e.ln(ny); int sz = l.sz();
      if (Key.ctrl(mod)) {
        while (nx>=1 && !Character.isWhitespace(l.get(nx-1)) && !EditNode.isName(l.get(nx-1))) nx--;
        while (nx>=1 && Character.isWhitespace(l.get(nx-1))) nx--;
        while (nx>=1 && EditNode.isName(l.get(nx-1))) nx--;
        if (nx==ex) nx--;
      } else nx--;
      if (nx<sz && Character.isLowSurrogate(l.get(nx))) nx--;
    }
    mvU(Key.shift(mod), nx, ny);
  }
  public void right(int mod) { px=-1;
    if (!Key.shift(mod) && sel()) { order(); mvU(ex, ey); return; }
    int nx=ex, ny=ey;
    if (nx+1>e.ln(ny).sz()) {
      if (ny!=e.lns.sz-1) {
        ny = Math.min(ny+1, e.lns.sz-1);
        nx = 0;
      }
    } else {
      EditNode.Line l = e.ln(ny); int sz = l.sz();
      if (Key.ctrl(mod)) {
        while (nx<sz && !Character.isWhitespace(l.get(nx)) && !EditNode.isName(l.get(nx))) nx++;
        while (nx<sz && Character.isWhitespace           (l.get(nx))) nx++;
        while (nx<sz && EditNode.isName(l.get(nx))) nx++;
        if (nx==ex) nx++;
      } else nx++;
      if (nx<sz && Character.isLowSurrogate(l.get(nx))) nx++;
    }
    mvU(Key.shift(mod), nx, ny);
  }
  public void up(int mod) { if (Key.ctrl(mod)) return;
    int nx=ex, ny=ey;
    if (ny==0) nx = 0;
    else nx = ud(e.ln(ny), e.ln(--ny), nx); // TODO moving up/down a wrapped line
    mvU(Key.shift(mod), nx, ny);
  }
  public void down(int mod) { if (Key.ctrl(mod)) return;
    int nx=ex, ny=ey;
    if (ny==e.lns.sz-1) nx = e.lns.peek().sz();
    else nx = ud(e.ln(ny), e.ln(++ny), nx); // ↑
    mvU(Key.shift(mod), nx, ny);
  }
  public int ud(EditNode.Line p, EditNode.Line n, int xp) {
    int xn = p.real(xp).x;
    if (px==-1) px = xn;
    else xn = Math.max(xn, px);
    return fix(Math.min(n.findX(xn, 0), n.sz()), n);
  }
  public int fix(int x, EditNode.Line l) {
    if (x<l.sz() && Character.isLowSurrogate(l.get(x))) return x+1;
    return x;
  }
  public void end(int mod) { px = -1;
    int ny=ey;
    if (Key.ctrl(mod)) ny = e.lns.sz-1;
    mvU(Key.shift(mod), e.ln(ny).sz(), ny);
  }
  public void home(int mod) { px = -1;
    int ny=ey;
    if (Key.ctrl(mod)) ny = 0;
    mvU(Key.shift(mod), 0, ny);
  }
  
  
  // graphics
  public void draw(Graphics g) {
    EditNode.Line ln = e.ln(ey);
    XY r = ln.real(ex);
    int h = e.f.hi;
    int y = ln.yw*h + r.y;
    g.rect(r.x, y, r.x+1, y+h, 0xffD2D2D2); // TODO theme (both color & width)
  }
  
  public int ym() { return Math.min(sy, ey); }
  public int yM() { return Math.max(sy, ey); }
  public int xm() { return sy==ey? Math.min(sx,ex) : ey>sy? sx : ex; }
  public int xM() { return sy==ey? Math.max(sx,ex) : ey>sy? ex : sx; }
  
  public int compareTo(Cursor o) {
    int ty = ym(), oy = o.ym();
    if (ty!=oy) return ty-oy;
    int tx = xm(), ox = o.xm();
    return tx-ox;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof Cursor)) return false;
    Cursor that = (Cursor) o;
    return this.sx == that.sx && this.sy == that.sy
        && this.ex == that.ex && this.ey == that.ey;
  }
}
