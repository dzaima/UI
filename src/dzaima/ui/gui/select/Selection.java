package dzaima.ui.gui.select;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;

public class Selection {
  public final int depth;
  public final Position a, b;
  public final Position.Spec aS, bS;
  public final Node c;
  public Position.Spec sS, eS;
  
  public Selection(int depth, Position a, Position b, Position.Spec aS, Position.Spec bS) {
    this.depth = depth;
    this.a = a;
    this.b = b;
    this.aS = aS;
    this.bS = bS;
    c = aS.sn;
    assert aS.sn == bS.sn;
  }
  
  public void end() {
    c.mRedraw();
    if (c instanceof InlineNode) {
      InlineNode.scanSelection(this, new InlineNode.SubSelConsumer() {
        public void addString(StringNode nd, int s, int e) {
          nd.flags&= ~StringNode.FLS_SEL;
        }
        public void addNode(Node nd) { }
      });
    }
  }
  
  public void start() {
    c.mRedraw();
    if (c instanceof InlineNode) {
      boolean rev = InlineNode.scanSelection(this, new InlineNode.SubSelConsumer() {
        public void addString(StringNode nd, int s, int e) {
          nd.flags|= StringNode.FL_SEL;
        }
        public void addNode(Node nd) { }
      });
      if (rev) { sS=bS; eS=aS; }
      else     { sS=aS; eS=bS; }
    }
  }
}
