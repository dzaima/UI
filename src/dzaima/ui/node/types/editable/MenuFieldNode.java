package dzaima.ui.node.types.editable;

import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.MenuNode;

public class MenuFieldNode extends TextFieldNode {
  public MenuFieldNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public Runnable onModified;
  public void onModified() {
    if (onModified!=null) onModified.run();
  }
  
  public boolean enter(int mod) {
    Node b = ctx.vw().base;
    if (b instanceof MenuNode) {
      ((MenuNode) b).obj.close();
    }
    return super.enter(mod);
  }
}
