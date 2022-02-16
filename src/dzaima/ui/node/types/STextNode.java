package dzaima.ui.node.types;

import dzaima.ui.eval.Token;
import dzaima.ui.gui.NodeWindow;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.Tools;

public class STextNode extends TextNode implements Selectable {
  public STextNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public STextNode(Node n) {
    super(n.ctx, KS_NONE, VS_NONE);
    add(n);
  }
  
  public boolean selectS(Selection s) {
    mRedraw();
    return InlineNode.scanSelection(s, new InlineNode.SubSelConsumer() {
      public void addString(StringNode nd, int s, int e) {
        nd.flags|= StringNode.FL_SEL;
      }
      public void addNode(Node nd) { }
    });
  }
  
  public void selectE(Selection s) {
    mRedraw();
    InlineNode.scanSelection(s, new InlineNode.SubSelConsumer() {
      public void addString(StringNode nd, int s, int e) {
        nd.flags&= ~StringNode.FLS_SEL;
      }
      public void addNode(Node nd) { }
    });
  }
  
  public String selType() {
    return "text";
  }
  
  public void forceStart(int x, int y, Click c) {
    c.notify(this, x, y);
    NodeWindow w = ctx.win();
    w.startSelection(Position.getPosition(this, x, y));
    w.focus(this);
  }
  public boolean mouseDown(int x, int y, Click c) {
    if (super.mouseDown(x, y, c)) return true;
    if (Key.any(c.mod)) return false;
    forceStart(x, y, c);
    return true;
  }
  
  public void mouseTick(int x, int y, Click c) {
    ctx.win().continueSelection(Position.getPosition(this, x, y));
  }
  
  public void focusE() {
    NodeWindow w = ctx.win();
    if (w.selection!=null && w.selection.c==this) {
      w.endSelection();
    }
  }
  
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    switch (gc.keymap(key, a, "stext")) {
      case "copy":
        NodeWindow w = ctx.win();
        if (w.selection!=null) w.copyString(InlineNode.getSelection(w.selection));
        return true;
    }
    return false;
  }
}
