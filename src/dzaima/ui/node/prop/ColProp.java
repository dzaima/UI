package dzaima.ui.node.prop;

import dzaima.utils.*;

public class ColProp extends PropI {
  
  private final int c;
  public ColProp(int c) {
    this.c = c;
  }
  
  public char type() { return '#'; }
  public int col() { return c; }
  
  public String toString() { return ColorUtils.format(c); }
  
  public boolean equals(Object o) { return o instanceof ColProp && c==((ColProp) o).c; }
}