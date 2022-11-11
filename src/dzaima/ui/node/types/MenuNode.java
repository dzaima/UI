package dzaima.ui.node.types;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
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
  
  public void focusPrev() {
    if (ch.sz==0) return;
    int i = ch.indexOf(ctx.win().focusNode())-1;
    if (i<0) i = ch.sz-1;
    ch.get(i).focusMe();
  }
  public void focusNext() {
    if (ch.sz==0) return;
    int i = ch.indexOf(ctx.win().focusNode())+1;
    if (i>=ch.sz) i = 0;
    ch.get(i).focusMe();
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
    if (a.press) {
      for (Node c : ch) {
        if (c instanceof MINode) {
          Vec<PNode> keys = ((MINode) c).keys();
          if (keys==null) continue;
          for (PNode k : keys) {
            if (key.equals(Key.byName(((PNode.PNodeStr) k).s))) {
              ((MINode) c).run();
              return true;
            }
          }
        }
      }
      switch (gc.keymap(key, a, "menu")) {
        case "prev": focusPrev(); return true;
        case "next": focusNext(); return true;
      }
    }
    return false;
  }
  
  public static class MINode extends Node {
    public MINode(Ctx ctx, String[] ks, Prop[] vs) {
      super(ctx, ks, vs);
    }
    private static final String[] ID_K = new String[]{"id"};
    public MINode(Ctx ctx, String ct, String id) {
      super(ctx, ID_K, new Prop[]{new EnumProp(id)});
      add(new StringNode(ctx, ct));
    }
    
    Font f;
    String binds;
    short padL, padR, padX, padU;
    float bindW;
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
          b.append(Objects.requireNonNull(Key.byName(((PNode.PNodeStr) n).s)).repr());
        }
        binds = b.length()==0? "(unassigned)" : b.toString();
        bindW = f.widthf(binds);
        padX+= bindW;
      } else {
        binds = null;
      }
    }
    
    public boolean keyF(Key key, int scancode, KeyAction a) {
      switch (gc.keymap(key, a, "menu")) {
        case "prev": menu().focusPrev(); return true;
        case "next": menu().focusNext(); return true;
        case "accept": run(); return true;
      }
      return false;
    }
    
    public int maxW() { return Tools.BIG; }
    public int minW(     ) { return ch.get(0).minW()+padX; }
    public int minH(int w) { return ch.get(0).minH(w-padX) + 2*padU; }
    public int maxH(int w) { return ch.get(0).maxH(w-padX) + 2*padU; }
    
    public void bg(Graphics g, boolean full) {
      if (hovered || focused) g.rect(0, 0, w, h, gc.getProp("menu.hover").col());
      else super.bg(g, full);
    }
    public void drawC(Graphics g) {
      if (binds!=null) StringNode.text(g, binds, f, 0xff888888, w-padR-bindW, padU+f.ascI);
    }
    
    boolean hovered;
    public void hoverS() { mRedraw(); hovered = true; }
    public void hoverE() { mRedraw(); hovered = false; }
    boolean focused;
    public void focusS() { super.focusS(); focused = true; }
    public void focusE() { super.focusE(); focused = false; }
    
    public MenuNode menu() { return (MenuNode) ctx.vw().base; }
    public void mouseStart(int x, int y, Click c) { c.register(this, x, y); }
    public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
    public void mouseUp(int x, int y, Click c) { if (visible) run(); }
    public void run() {
      menu().obj.menuItem(vs[id("id")].val());
    }
    
    public void resized() {
      Node c = ch.get(0);
      c.resize(Math.min(c.maxW(), w-padX), h-2*padU, padL, padU);
      mRedraw();
    }
  }
}
