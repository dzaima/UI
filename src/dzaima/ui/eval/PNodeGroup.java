package dzaima.ui.eval;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.*;

public class PNodeGroup extends PNode {
  public final String name;
  public final boolean defn;
  public final String[] ks;
  public final Prop[] vs; // only properties which are contant known; contains null elements otherwise
  public final Props staticProps; // non-null only if all props are known
  
  public final Vec<PrsField> props;
  public final Vec<PNode> ch;
  
  public PNodeGroup(String name, boolean defn, Vec<PrsField> props, Vec<PNode> ch) {
    this.name = name;
    this.defn = defn;
    this.props = props; int sz = props.sz;
    ks = sz==0? Node.KS_NONE : new String[sz];
    vs = sz==0? Node.VS_NONE : new Prop  [sz];
    boolean allKnown = true;
    for (int i = 0; i < sz; i++) {
      ks[i] = props.get(i).name;
      vs[i] = Prop.makeConstProp(props.get(i));
      allKnown&= vs[i]!=null;
    }
    staticProps = allKnown? Props.ofKV(ks, vs) : null;
    if (Tools.DBG) {
      HashSet<String> seen = new HashSet<>();
      for (String c : ks) {
        if (!seen.add(c)) throw new AssertionError("Duplicate key in "+Arrays.toString(ks)+": "+c);
      }
    }
    this.ch = ch;
  }
  
  public PNodeGroup copy() {
    assert !defn;
    return new PNodeGroup(name, defn, new Vec<>(props), new Vec<>(ch));
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
