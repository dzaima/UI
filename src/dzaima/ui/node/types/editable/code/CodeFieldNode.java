package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.undo.UndoManager;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class CodeFieldNode extends CodeAreaNode {
  public CodeFieldNode(Ctx ctx, String[] ks, Prop[] vs) {
    this(ctx, ks, vs, new UndoManager(ctx.gc));
    lineNumbering = false;
  }
  public CodeFieldNode(Ctx ctx, String[] ks, Prop[] vs, UndoManager um) {
    super(ctx, ks, vs, false, um);
    lineNumbering = false;
  }
}
