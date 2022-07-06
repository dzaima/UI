package dzaima.ui.node.prop;

import dzaima.ui.gui.config.GConfig;
import dzaima.utils.Tools;

public class RangeProp extends PropI {
  private final double s, e;
  private final int pxS, pxE;
  private final String t;
  public final boolean needsEm;
  private final GConfig gc;
  
  
  public RangeProp(GConfig gc, double s, double e, String t) {
    this.gc = gc;
    this.s = s;
    this.e = e;
    this.t = t;
    needsEm = t.equals("em") && (s!=0 | e!=0);
    pxS = needsEm? -1 : Tools.ceil(s);
    pxE = needsEm? -1 : Tools.ceil(e);
  }
  
  
  public char type() { return ':'; }
  
  public int lenS() { return needsEm? Tools.ceil(s*gc.em) : pxS; }
  public int lenE() { return needsEm? Tools.ceil(e*gc.em) : pxE; }
  public RangeProp range() { return this; }
  
  public String toString() {
    if (t.equals("px")) return s+t+":"+e+t;
    return s+t+":"+e+t+" = "+lenS()+"px:"+lenE()+"px";
  }
  public boolean equals(Object o0) {
    if (!(o0 instanceof RangeProp)) return false;
    RangeProp o = (RangeProp) o0;
    return s==o.s && e==o.e && t.equals(o.t);
  }
}
