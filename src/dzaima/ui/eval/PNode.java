package dzaima.ui.eval;

import dzaima.utils.JSON;

public abstract class PNode {
  public abstract String toString(String pad);
  
  public String toString() {
    return toString("");
  }
  
  public static class PNodeStr extends PNode {
    public final String s;
    public PNodeStr(String s) { this.s = s; }
    
    public String toString(String pad) { return pad+JSON.quote(s)+"\n"; }
  }
  
  public static class PNodeDefn extends PNode {
    public final String s;
    public PNodeDefn(String s) { this.s = s; }
    
    public String toString(String pad) { return pad+"def:"+JSON.quote(s)+"\n"; }
  }
}
