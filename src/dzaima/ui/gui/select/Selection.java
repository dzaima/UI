package dzaima.ui.gui.select;

import dzaima.ui.node.Node;

public class Selection {
  public final Position a, b; // complete selection
  public final int depth; // agreed depth
  public final Selectable c; // owner at depth
  public final PosPart aS, bS; // selection points at depth
  public PosPart sS, eS; // selection sorted
  
  public Selection(int depth, Position a, Position b, PosPart aS, PosPart bS) {
    this.depth = depth;
    this.a = a;
    this.b = b;
    this.aS = aS;
    this.bS = bS;
    c = aS.sn;
    assert aS.sn == bS.sn;
  }
  
  public void started() {
    c.selectS(this);
  }
  public void setSorted(boolean rev) {
    if (rev) { sS=bS; eS=aS; }
    else     { sS=aS; eS=bS; }
  }
  
  public void ended() {
    c.selectE(this);
  }
  
  public void end() {
    ((Node) c).ctx.win().endSelection();
  }
}
