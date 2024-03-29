package dzaima.ui.apps.devtools;

import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.StringNode;
import dzaima.ui.node.types.tree.TNNode;
import dzaima.utils.Log;

import java.util.HashMap;

public class DTTNNode extends TNNode { // devtools tree node node
  public static final Props DT_PROPS = Props.of("family", new StrProp("DejaVu Sans Mono"));
  public final Devtools d;
  public final Node insp;
  
  public DTTNNode(Devtools d, Ctx ctx, Node insp) {
    super(ctx, DT_PROPS, false, true);
    this.d = d;
    this.insp = insp;
    add(new StringNode(ctx, "?"));
    dtUpd();
  }
  
  public void dtUpd() {
    String text = Devtools.name(insp, true);
    
    replace(0, new StringNode(ctx, text));
    if (open) {
      if (!openable) { close(); return; }
      HashMap<Node, DTTNNode> prev = new HashMap<>();
      for (int i = 1; i < ch.sz; i++) {
        DTTNNode c = (DTTNNode) ch.get(i);
        prev.put(c.insp, c);
      }
      remove(1, ch.sz);
      for (Node c : insp.ch) {
        Node p = prev.get(c);
        add(p!=null? p : new DTTNNode(d, ctx, c));
      }
    } else {
      openable = insp.ch.sz!=0;
      remove(1, ch.sz);
    }
  }
  
  public void open() { super.open();
    dtUpd();
  }
  
  public void close() { super.close();
    dtUpd();
  }
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    if (a.release) return false;
    if (key.k_del()) {
      if (insp.p==null || !up()) return false;
      insp.ctx.win().enqueue(() -> {
        int i = insp.p.ch.indexOf(insp);
        if (i!=-1) {
          try {
            insp.p.remove(i, i+1);
          } catch (Throwable t) {
            Log.error("devtools", "Error while removing node; forcibly removing");
            i = insp.p.ch.indexOf(insp);
            if(i!=-1) insp.p.ch.remove(i, i+1);
          }
        }
        else Log.warn("devtools", "Node to be removed not found");
      });
      return true;
    }
    return super.keyF(key, scancode, a);
  }
}