package dzaima.ui.node.prop;

import dzaima.utils.Tools;

public class ColProp extends PropI {
  
  private final int c;
  public ColProp(int c) {
    this.c = c;
  }
  
  public char type() { return '#'; }
  public int col() { return c; }
  
  public String toString() { return Tools.fmtColor(c); }
}