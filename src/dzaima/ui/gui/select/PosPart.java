package dzaima.ui.gui.select;

import dzaima.ui.node.Node;

public class PosPart { // a reference to a specific selection's start or end
  public final int depth;
  public final Selectable sn; // selection root
  public Node ln; // selected node
  public int pos; // position within selected node; 0 or 1 for "v"/"h", character offset for "text"
  
  public PosPart(int depth, Selectable sn, int pos, Node ln) {
    this.depth = depth;
    this.sn = sn;
    this.pos = pos; // potentially overwritten later
    this.ln = ln; // potentially overwritten later
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof PosPart)) return false;
    PosPart c = (PosPart) o;
    return pos==c.pos && sn==c.sn && ln==c.ln;
  }
}
