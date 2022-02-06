package dzaima.ui.node.prop;

import dzaima.utils.JSON;

public class StrProp extends PropI {
  public final String s;
  
  public StrProp(String s) {
    this.s = s;
  }
  
  public char type() { return '"'; }
  public String str() { return s; }
  
  public String toString() {
    return JSON.quote(s);
  }
}
