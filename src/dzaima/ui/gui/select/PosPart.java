package dzaima.ui.gui.select;

import dzaima.ui.node.Node;

public class PosPart {
  public final int depth;
  public final Selectable sn; // selection root
  public Node ln; // selected node
  public int pos; // position within selected node
  
  public PosPart(int depth, Selectable sn, int pos) {
    this.depth = depth;
    this.pos = pos; // potentially overwritten later
    this.sn = sn;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof PosPart)) return false;
    PosPart c = (PosPart) o;
    return pos==c.pos && sn==c.sn && ln==c.ln;
  }
}
