package dzaima.ui.node.prop;

import dzaima.ui.gui.config.GConfig;
import dzaima.utils.Tools;

public class LenProp extends PropI {
  private final double d;
  private final int px;
  public final String t;
  public final GConfig gc;
  public boolean needsEm;
  
  public LenProp(GConfig gc, double d, String t) {
    this.gc = gc;
    this.d = d;
    this.t = t;
    needsEm = t.equals("em") && d!=0;
    px = needsEm? -1 : Tools.ceil(d);
  }
  
  public char type() { return 'l'; }
  public int len() { return needsEm? Tools.ceil(d*gc.em) : px; }
  public float lenF() { return needsEm? (float)(d*gc.em) : px; }
  
  public String toString() {
    if (t.equals("px") || gc==null) return d+t;
    return d+t+"="+len()+"px";
  }
  public boolean equals(Object o) { return o instanceof LenProp && d==((LenProp) o).d && t.equals(((LenProp) o).t); }
  
  public double reallyGetRaw() { return d; }
}
