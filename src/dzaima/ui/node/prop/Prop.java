package dzaima.ui.node.prop;

import dzaima.ui.eval.*;
import dzaima.ui.eval.PrsField.*;
import dzaima.ui.gui.config.GConfig;

public abstract class Prop {
  public abstract char type();
  
  public double  d  () { throw new RuntimeException("This is not a double!"); }
  public float   f  () { return (float) d(); }
  public int     i  () { return (int) d(); }
  public int     len() { throw new RuntimeException("This is not a length!"); }
  public float  lenF() { throw new RuntimeException("This is not a length!"); }
  public int     col() { throw new RuntimeException("This is not a color!"); }
  public String  str() { throw new RuntimeException("This is not a string!"); }
  public String  val() { throw new RuntimeException("This is not a value!"); }
  public <T> T   obj() { throw new RuntimeException("This is not an object!"); }
  public RangeProp range() { throw new RuntimeException("This is not a range!"); }
  public PNodeGroup gr() { throw new RuntimeException("This is not a group!"); }
  public boolean b() {
    char t = type();
    if (t=='0') return i()!=0;
    if (t=='a') {
      String v = val();
      if (v.equals("true")) return true;
      if (v.equals("false")) return false;
    }
    throw new RuntimeException(this+" isn't a bool");
  }
  
  public boolean isNull() { return false; }
  
  
  
  
  public static Prop makeProp(GConfig gc, PrsField val) {
    if (val instanceof NumFld) {
      double d = ((NumFld) val).d;
      String t = ((NumFld) val).t;
      if (t.equals("px") || t.equals("em")) {
        return new LenProp(gc, d, t);
      }
      if (t.length()==0) return new NumProp(d);
    }
    if (val instanceof ColFld) {
      return new ColProp(((ColFld) val).c);
    }
    if (val instanceof NameFld) {
      NameFld c = (NameFld) val;
      if (c.cfg) return gc.getCfgProp(c.s);
      else       return new EnumProp(c.s);
    }
    if (val instanceof StrFld) {
      return new StrProp(((StrFld) val).s);
    }
    if (val instanceof RangeFld) {
      RangeFld c = (RangeFld) val;
      return new RangeProp(gc, c.s, c.e, c.t);
    }
    if (val instanceof GroupFld) {
      return new GrProp(((GroupFld) val).g);
    }
    throw new RuntimeException("PVal "+val.getClass().getSimpleName()+" nyi");
  }
  
  public static Prop makeConstProp(PrsField val) {
    if (val instanceof NumFld) {
      double d = ((NumFld) val).d;
      String t = ((NumFld) val).t;
      if (t.equals("px")) return new LenProp(null, d, t);
      if (t.length()==0) return new NumProp(d);
    }
    if (val instanceof ColFld) {
      return new ColProp(((ColFld) val).c);
    }
    if (val instanceof NameFld) {
      NameFld c = (NameFld) val;
      if (c.cfg) return null;
      return new EnumProp(c.s);
    }
    if (val instanceof StrFld) {
      return new StrProp(((StrFld) val).s);
    }
    return null;
  }
}
