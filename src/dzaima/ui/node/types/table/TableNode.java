package dzaima.ui.node.types.table;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

import java.util.Arrays;

public class TableNode extends Node { // TODO table is very unfinished
  
  public TableNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  public boolean clip;
  public int bg1, bg2, bgH, bgSel, sepCol, sepY;
  public int padX, sep, colDist, padY;
  public boolean rowSel;
  public void propsUpd() { super.propsUpd();
    clip = gc.boolD(this, "clip", false);
    rowSel = gc.boolD(this, "rowSel", false);
    
    sep = gc.emD(this, "sep", 0);
    sepCol = gc.col(this, "sepCol", "hsep.color");
    sepY = gc.len(this, "sepY", "hsep.y");
    
    padX = gc.emD(this, "padX", 0);
    padY = gc.emD(this, "padY", 0);
    colDist = padX*2 + sep;
    
    Integer bg = gc.col(this, "bg");
    if (bg!=null) {
      bg1=bg2 = bg;
    } else {
      bg1 = gc.col(this, "bg1", "table.bg1");
      bg2 = gc.col(this, "bg2", "table.bg2");
    }
    bgH = gc.col(this, "bgH", "table.bgH");
    bgSel = gc.col(this, "bgSel", "bg.sel");
  }
  int mW=-1, mH, hW=-1;
  private float[] preferred; // must sum to 1; must either be null or have the length of any row length
  private THNode th; private boolean thI;
  public void mChResize() { super.mChResize();
    mW=hW=-1;
    colsW=-1;
    thI = false;
  }
  public THNode th() {
    if (thI) return th;
    thI = true;
    if (ch.sz==0) return th=null;
    Node c = ch.get(0);
    if (c instanceof THNode) return th = (THNode) c;
    return th=null;
  }
  public void setPreferred(float[] preferred) {
    this.preferred = preferred;
    mResize();
  }
  
  public void drawCh(Graphics g, boolean full) {
    for (int i = th()==null?0:1; i < ch.sz; i++) {
      Node c = ch.get(i);
      if (c.dy+c.h >= g.clip.sy && c.dy<g.clip.ey) c.draw(g, full);
    }
  }
  public void over(Graphics g) {
    THNode th = th();
    if (th!=null) th.draw(g, true);
  }
  
  private int[] cols;
  private int colsW=-1;
  public int[] cols(int w) {
    if (w==colsW) return cols;
    colsW = w;
    if (ch.sz==0) return cols=new int[0];
    int[] r = new int[ch.get(0).ch.sz];
    w-= r.length*colDist - sep;
    if (preferred!=null) {
      for (int c = 0; c < r.length; c++) {
        r[c] = (int) (preferred[c]*w); // floor to prevent overflow
      }
    } else {
      int p = w/r.length;
      Arrays.fill(r, p);
      r[r.length-1] = w-p*(r.length-1);
    }
    if (!clip) for (Node c : ch) {
      for (int i = 0; i < c.ch.sz; i++) {
        Node cc = c.ch.get(i);
        r[i] = Math.max(r[i], cc.minW());
      }
    }
    return cols=r;
  }
  
  public int minW() {
    if (mW<0) {
      if (clip) return 0;
      int[] cols = cols(w==-1? 0 : w); // TODO this shouldn't be using w
      mW = cols.length*colDist-sep;
      for (int c : cols) mW+= c;
    }
    return mW;
  }
  public int minH(int w) {
    if (hW!=w) {
      mH = padY*2*ch.sz;
      hW = w;
      for (Node c : ch) mH+= c.minH(w);
    }
    return mH;
  }
  
  public void resized() {
    int y = 0;
    int pos = 0;
    boolean th = th()!=null;
    if (th) {
      THNode c = th();
      int h = c.minH(w)+padY*2;
      c.resize(w, h, 0, c.sticky!=null?c.dy:0);
      y+= h;
    }
    for (int i = th?1:0; i < ch.sz; i++) {
      Node c = ch.get(i);
      int h = c.minH(w)+padY*2;
      c.resize(w, h, 0, y);
      y+= h;
      if (c instanceof TRNode) {
        ((TRNode) c).pos = pos++;
      } else assert i==0 : "Didn't expect "+c.getClass().getSimpleName()+" at "+i;
    }
  }
}
