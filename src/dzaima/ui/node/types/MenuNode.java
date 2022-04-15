package dzaima.ui.node.types;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

import java.util.Objects;

public class MenuNode extends Node {
  public Popup obj;
  
  public MenuNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  short padY;
  public void propsUpd() { super.propsUpd();
    padY = (short) gc.getProp("menu.wholePadY").len();
  }
  
  public int minW() { return Math.max(Solve.vMinW(ch), gc.getProp("menu.minWidth").len()); }
  public int minH(int w) { return Solve.vMinH(ch, w) + padY*2; }
  
  public void bg(Graphics g, boolean full) {
    g.clear(gc.getProp("menu.bg").col());
  }
  
  protected void resized() {
    int y = padY;
    for (Node c : ch) {
      int cw = Math.min(w, c.maxW());
      int ch = c.minH(cw);
      c.resize(cw, ch, 0, y);
      y+= ch;
    }
    mRedraw();
  }
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    for (Node c : ch) {
      if (c instanceof MINode) {
        Vec<PNode> keys = ((MINode) c).keys();
        if (keys==null) continue;
        for (PNode k : keys) {
          if (key.equals(Key.byName(((PNodeStr) k).s))) {
            ((MINode) c).run();
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public static class MINode extends Node {
    public MINode(Ctx ctx, String[] ks, Prop[] vs) {
      super(ctx, ks, vs);
    }
    
    Font f;
    String binds;
    short padL, padR, padX, padU, bindW;
    public Vec<PNode> keys() {
      int ki = id("key");
      if (ki==-1) return null;
      return vs[ki].gr().ch;
    }
    public void propsUpd() { super.propsUpd();
      padL = (short) gc.getProp("menu.padL").len();
      padR = (short) gc.getProp("menu.padR").len();
      padU = (short) gc.getProp("menu.padY").len();
      padX = (short) (padL+padR);
  
      Vec<PNode> keys = keys();
      if (keys!=null) {
        f = gc.defFont.size(gc.getProp("menu.keybindSize").len());
        StringBuilder b = new StringBuilder();
        for (PNode n : keys) {
          if (b.length()>0) b.append(", ");
          b.append(Objects.requireNonNull(Key.byName(((PNodeStr) n).s)).repr());
        }
        binds = b.length()==0? "(unassigned)" : b.toString();
        bindW = (short) f.width(binds);
        padX+= bindW;
      } else {
        binds = null;
      }
    }
    
    public int maxW() { return Tools.BIG; }
    public int minW(     ) { return ch.get(0).minW()+padX; }
    public int minH(int w) { return ch.get(0).minH(w-padX) + 2*padU; }
    public int maxH(int w) { return ch.get(0).maxH(w-padX) + 2*padU; }
    
    public void bg(Graphics g, boolean full) {
      if (hovered) g.rect(0, 0, w, h, gc.getProp("menu.hover").col());
      else super.bg(g, full);
    }
    public void drawC(Graphics g) {
      if (binds!=null) StringNode.text(g, binds, f, 0xff888888, w-padR-bindW, padU+f.ascI);
    }
  
    boolean hovered;
    public void hoverS() { mRedraw(); hovered = true; }
    public void hoverE() { mRedraw(); hovered = false; }
    
    public void mouseStart(int x, int y, Click c) { c.register(this, x, y); }
    public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
    public void mouseUp(int x, int y, Click c) { run(); }
    public void run() {
      ((MenuNode) ctx.vw().base).obj.menuItem(vs[id("id")].val());
    }
    
    public void resized() {
      Node c = ch.get(0);
      c.resize(Math.min(c.maxW(), w-padX), h-2*padU, padL, padU);
      mRedraw();
    }
  }
}
