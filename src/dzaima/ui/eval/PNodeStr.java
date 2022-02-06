package dzaima.ui.eval;

import dzaima.utils.JSON;

public class PNodeStr extends PNode {
  public final String s;
  public PNodeStr(String s) {
    this.s = s;
  }
  
  public String toString(String pad) {
    return pad+JSON.quote(s)+"\n";
  }
}
