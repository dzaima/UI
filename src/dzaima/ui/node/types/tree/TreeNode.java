package dzaima.ui.node.types.tree;

import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

public class TreeNode extends ATreeNode {
  public boolean defaultClosed = true;
  
  
  public int indent, bg1, bg2, bgSel;
  public TreeNode(Ctx ctx, Props props) {
    super(ctx, props, 0);
    depth = 0;
    base = this;
  }
  public void propsUpd() { super.propsUpd();
    bgSel = gc.col(this, "bgSel", "bg.sel");
    indent = gc.emD(this, "indent", 1);
    
    Integer bg = gc.col(this, "bg");
    if (bg!=null) {
      bg1=bg2 = bg;
    } else {
      bg1 = gc.col(this, "bg1", "tree.bg1");
      bg2 = gc.col(this, "bg2", "tree.bg2");
    }
  }
  public int indentFor(int depth) {
    return indent;
  }
  
  
  
  // TODO cache?
  public int minW() {
    int w = 0;
    for (Node c : ch) w = Math.max(w, c.minW());
    return w;
  }
  public int minH(int w) {
    int h = 0;
    for (Node c : ch) h += c.minH(w);
    return h;
  }
  public int maxH(int w) { return minH(w); }
  
  public void resized() {
    int y = 0;
    for (Node c : ch) {
      int ch = c.minH(w);
      c.resize(w, ch, 0, y);
      y+= ch;
    }
    recalcEven(false);
  }
}
