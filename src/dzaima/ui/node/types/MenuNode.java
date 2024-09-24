package dzaima.ui.node.types;

import dzaima.ui.eval.PNode;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.utils.*;
import dzaima.utils.*;

import java.util.Objects;

public class MenuNode extends Node {
  public Popup obj;
  
  public MenuNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  short padY;
  public void propsUpd() { super.propsUpd();
    padY = (short) gc.getProp("menu.wholePadY").len();
  }
  
  public void focusDelta(int d) {
    if (ch.sz==0) return;
    int i = ch.indexOf(ctx.win().focusNode());
    for (int j = 0; j < ch.sz; j++) { // bounded loop just to be safe
      i = Math.floorMod(i+d, ch.sz);
      if (ch.get(i) instanceof MINode) { ch.get(i).focusMe(); return; }
    }
  }
  public void focusPrev() {
    focusDelta(-1);
  }
  public void focusNext() {
    focusDelta(1);
  }
  
  public int minW() { return Math.max(ListUtils.vMinW(ch), gc.getProp("menu.minWidth").len()); }
  public int minH(int w) { return ListUtils.vMinH(ch, w) + padY*2; }
  
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
    public MINode(Ctx ctx, Props props) {
      super(ctx, props);
    }
    public MINode(Ctx ctx, String ct, String id) {
      super(ctx, Props.of("id", new EnumProp(id)));
      add(new StringNode(ctx, ct));
    }
    
    Font f;
    String binds;
    short padL, padR, padX, padU;
    float bindW;
    public Vec<PNode> keys() {
      Prop k = getPropN("key");
      return k==null? null : k.gr().ch;
    }
    public void propsUpd() { super.propsUpd();
      padL = (short) gc.getProp("menu.padL").len();
      padR = (short) gc.getProp("menu.padR").len();
      padU = (short) gc.getProp("menu.padY").len();
      padX = (short) (padL+padR);
      
      Vec<PNode> keys = keys();
      if (keys!=null && keys.sz>0) {
        f = gc.defFont.size(gc.getProp("menu.keybindSize").len());
        StringBuilder b = new StringBuilder();
        for (PNode n : keys) {
          if (b.length()>0) b.append(", ");
          b.append(Objects.requireNonNull(Key.byName(((PNode.PNodeStr) n).s)).repr());
        }
        binds = b.toString();
        bindW = f.widthf(binds);
        padX+= (short) bindW;
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
      menu().obj.menuItem(getProp("id").val());
    }
    
    public void resized() {
      Node c = ch.get(0);
      c.resize(Math.min(c.maxW(), w-padX), h-2*padU, padL, padU);
      mRedraw();
    }
  }
}
