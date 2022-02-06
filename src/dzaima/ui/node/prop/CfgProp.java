package dzaima.ui.node.prop;

import dzaima.ui.eval.PNodeGroup;

public class CfgProp extends Prop {
  public char type;
  public PropI v;
  
  public CfgProp(PropI v) {
    init(v);
  }
  
  public char      type () { return type; }
  public double    d    () { return v.d    (); }
  public int       len  () { return v.len  (); }
  public float     lenF () { return v.lenF (); }
  public int       col  () { return v.col  (); }
  public String    str  () { return v.str  (); }
  public String    val  () { return v.val  (); }
  public RangeProp range() { return v.range(); }
  public <T> T     obj  () { return v.obj  (); }
  public PNodeGroup gr  () { return v.gr   (); }
  
  public boolean  isNull() { return v.isNull(); }
  
  public void init(PropI v) {
    this.type = v.type();
    this.v = v;
  }
  public void erase() {
    this.type = '\0';
    this.v = null;
  }
}
