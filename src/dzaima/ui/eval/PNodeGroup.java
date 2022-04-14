package dzaima.ui.eval;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

import java.util.*;

public class PNodeGroup extends PNode {
  public final String name;
  public final String[] ks;
  public final Prop[] vs; // only properties which are contant known; contains null elements otherwise
  
  public final Vec<PrsField> props;
  public final Vec<PNode> ch;
  
  public PNodeGroup(String name, Vec<PrsField> props, Vec<PNode> ch) {
    this.name = name;
    this.props = props; int sz = props.sz;
    ks = sz==0? Node.KS_NONE : new String[sz];
    vs = sz==0? Node.VS_NONE : new Prop  [sz];
    for (int i = 0; i < sz; i++) {
      ks[i] = props.get(i).name;
      vs[i] = Prop.makeConstProp(props.get(i));
    }
    if (Tools.DBG) {
      HashSet<String> seen = new HashSet<>();
      for (String c : ks) {
        if (!seen.add(c)) throw new AssertionError("Duplicate key in "+Arrays.toString(ks)+": "+c);
      }
    }
    this.ch = ch;
  }
  
  public PNodeGroup copy() {
    return new PNodeGroup(name, new Vec<>(props), new Vec<>(ch));
  }
  
  public String toString(String pad) {
    StringBuilder s = new StringBuilder(pad+name+" {\n");
    String cpad = pad+"  ";
    for (PrsField c : props) {
      for (String ln : c.toString().split("\n")) {
        s.append(cpad).append(ln).append('\n');
      }
    }
    for (PNode c : ch) {
      s.append(c.toString(cpad));
    }
    s.append(pad).append("}\n");
    return s.toString();
  }
}
