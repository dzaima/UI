package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;

public class STextNode extends TextNode implements Selectable {
  public STextNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  private static String[] IBEAM_K = new String[]{"ibeam"};
  private static Prop[] IBEAM_V = new Prop[]{EnumProp.TRUE};
  public STextNode(Ctx ctx, boolean ibeam) {
    super(ctx, ibeam? IBEAM_K : KS_NONE, ibeam? IBEAM_V : VS_NONE);
  }
  
  public void selectS(Selection s) {
    mRedraw();
    s.setSorted(InlineNode.scanSelection(s, new InlineNode.SubSelConsumer() {
      public void addString(StringNode nd, int s, int e) {
        nd.flags|= StringNode.FL_SEL;
      }
      public void addNode(Node nd) { }
    }));
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
  
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (Key.none(c.mod0) && c.bL()) c.register(this, x, y);
  }
  
  public void mouseDown(int x, int y, Click c) {
    NodeWindow w = ctx.win();
    w.startFocusSelection(Position.getPosition(this, x, y));
  }
  
  public void mouseTick(int x, int y, Click c) {
    ctx.win().continueFocusSelection(Position.getPosition(this, x, y));
  }
  
  public void focusE() {
    ctx.win().invalidateSelection(this);
  }
  
  public void hoverS() { if (gc.boolD(this, "ibeam", false)) ctx.vw().pushCursor(Window.CursorType.IBEAM); }
  public void hoverE() { if (gc.boolD(this, "ibeam", false)) ctx.vw().popCursor(); }
  
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
