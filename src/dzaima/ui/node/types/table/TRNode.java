package dzaima.ui.node.types.table;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.utils.Tools;

import java.util.function.IntConsumer;

public class TRNode extends TRTNode {
  public TRNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  // 0:enter; 1:click; 2:double-click
  public /*open*/ void action(int mode) { if (fn!=null) fn.accept(mode); }
  protected IntConsumer fn;
  
  public int pos;
  public void bg(Graphics g, boolean full) {
    int c = hasBg? bg : (pos&1)==0? t.bg1 : t.bg2;
    if (ctx.focusedNode()==this) c = t.bgSel;
    if (Tools.st(c)) pbg(g, full);
    if (Tools.vs(c)) g.rect(0, 0, w, h, c);
  }
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (c.bL()) c.register(this, x, y);
  }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) {
    if (!visible) return;
    if (c.bL()) {
      if (t.rowSel) ctx.focus(this);
      if (c.onDoubleClick()) action(2);
      else if (gc.isClick(c)) action(1);
    }
  }
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    if (a.release || !key.plain()) return false;
    if (key.k_up() || key.k_down()) {
      int y = pos + (key.k_up()?-1:1);
      if (y<0) return true;
      if (t.th()!=null) y++;
      if (y<t.ch.sz) ctx.focus(t.ch.get(y));
      return true;
    }
    if (key.k_enter()) action(0); // TODO shouldn't this return true?
    return false;
  }
}
