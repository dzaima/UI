package dzaima.ui.eval;

import dzaima.ui.eval.Token.NumTok;
import dzaima.utils.*;

public abstract class PrsField {
  public abstract String toString();
  public final String name;
  PrsField(String name) { this.name = name; }
  
  public static class ColFld extends PrsField {
    public final int c;
    public ColFld(String n, int c) { super(n); this.c = c; }
    public String toString() { return name+" = "+ColorUtils.format(c); }
  }
  
  public static class NameFld extends PrsField {
    public final String s;
    public final boolean cfg;
    public NameFld(String n, String s) { super(n); this.s = s; cfg = s.indexOf('.')!=-1; }
    public String toString() { return name+" = "+s; }
  }
  
  public static class GroupFld extends PrsField {
    public final PNodeGroup g;
    public GroupFld(String n, PNodeGroup g) { super(n); this.g = g; }
    public String toString() { return name+" = "+g.toString(); }
  }
  
  public static class NumFld extends PrsField {
    public final double d;
    public final String t;
    public NumFld(String n, double d, String t) { super(n); this.d = d; this.t = t; }
    
    public String toString() { return name+" = "+d+t; }
  }
  
  public static class StrFld extends PrsField {
    public final String s;
    public StrFld(String n, String s) { super(n); this.s = s; }
    public String toString() { return name+" = "+JSON.quote(s); }
  }
  
  
  public static class RangeFld extends PrsField {
    public final double s, e;
    public final String t;
    public RangeFld(String n, NumTok s, NumTok e) {
      super(n);
      this.s = s.num;
      this.e = e.num;
      if (!s.s.equals(e.s)) throw new RuntimeException("Range ends must have same type");
      this.t = s.s;
    }
    public String toString() { return name+" = "+s+t+":"+e+t; }
  }
}
