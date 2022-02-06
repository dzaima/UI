package dzaima.ui.node.prop;

import dzaima.ui.eval.PNodeGroup;

public class GrProp extends PropI {
  public final PNodeGroup val;
  
  public GrProp(PNodeGroup val) {
    this.val = val;
  }
  
  public char type() { return '{'; }
  
  public PNodeGroup gr() {
    return val;
  }
  
  public String toString() {
    return "(group)";
  }
}
