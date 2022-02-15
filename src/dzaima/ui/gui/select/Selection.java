package dzaima.ui.gui.select;

import dzaima.ui.node.Node;

public class Selection {
  public final int depth;
  public final Position a, b;
  public final Position.Spec aS, bS;
  public final Node c;
  
  public Selection(int depth, Position a, Position b, Position.Spec aS, Position.Spec bS) {
    this.depth = depth;
    this.a = a;
    this.b = b;
    this.aS = aS;
    this.bS = bS;
    c = aS.sn;
    assert aS.sn == bS.sn;
  }
}
