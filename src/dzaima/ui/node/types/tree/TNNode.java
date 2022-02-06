package dzaima.ui.node.types.tree;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public class TNNode extends ATreeNode {
  public TNNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs, 1);
    openable = gc.boolD(this, "openable", true);
    open = openable && gc.boolD(this, "open", true);
  }
  public TNNode(Ctx ctx, String[] ks, Prop[] vs, boolean open, boolean openable) {
    super(ctx, ks, vs, 1);
    this.open = open;
    this.openable = openable;
    assert openable || !open;
  }
  
  // visibility stuff
  public void shown() {
    assert p instanceof ATreeNode : "`tn` should only be in `tn` or `tree`";
    ATreeNode pt = (ATreeNode) this.p;
    depth = pt.depth+1;
    base = pt.base;
    assert base!=null;
    super.shown(); // done afterwards so children already see my base
    if (Tools.DBG) for (int i = 0; i < ch.sz; i++) {
      assert ch.get(i) instanceof TNNode ^ i==0 : i==0? "first child of `tn` can't be `tn`" : "all but first child of `tn` must be `tn`";
    }
  }
  public void hidden() { super.hidden();
    depth = -1;
    base = null;
  }
  public void open() {
    if (open || !openable) return;
    open = true;
    if (base.defaultClosed) for (int i = 1; i < ch.sz; i++) ((TNNode) ch.get(i)).open = false;
    mResize();
  }
  public void close() {
    if (!open) return;
    open = false;
    mResize();
  }
  
  
  // drawing
  public void drawCh(Graphics g, boolean full) {
    if (!open) { ch.get(0).draw(g, full); return; }
    super.drawCh(g, full);
  }
  public /*open*/ int bgCol() {
    if (ctx.win().focusNode==this) return base.bgSel;
    return even? base.bg1 : base.bg2;
  }
  public void bg(Graphics g, boolean full) {
    if (base.bg1==base.bg2) { pbg(g, full); return; }
    g.rect(-((ATreeNode) p).totalIndent, 0, w, ch.get(0).h, bgCol());
  }
  
  
  // layout
  public int minW() { // TODO cache all of these?
    int ind = indent();
    int w = ch.get(0).minW();
    if (open) for (int i = 1; i < ch.sz; i++) w = Math.max(w, ch.get(i).minW() + ind);
    return w;
  }
  public int maxW() { return Tools.BIG; }
  public int minH(int w) {
    int ind = indent();
    int h = ch.get(0).minH(w);
    if (open) for (int i = 1; i < ch.sz; i++) h+= ch.get(i).minH(w - ind);
    return h;
  }
  public int maxH(int w) { return minH(w); }
  
  
  public void resized() {
    int ind = indent();
    totalIndent = ((ATreeNode) p).totalIndent+ind;
    int x = 0;
    int y = 0;
    for (int i = 0; i < (open?ch.sz:1); i++) {
      Node c = ch.get(i);
      int ch = c.minH(w-x);
      c.resize(w-x, ch, x, y);
      y+= ch;
      x = ind;
    }
  }
  
  public Node findCh(int x, int y) {
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      if (y<c.dy+c.h) return c;
    }
    return null;
  }
  
  
  // user interaction
  public boolean mouseDown(int x, int y, Click c) {
    if (y < ch.get(0).h) {
      boolean prev = ctx.win().focusNode==this;
      if (ctx.win().focusNode!=this) ctx.win().focus(this);
      if (ch.get(0).mouseDown(x, y, c)) return true; // TODO option for drag-selecting different nodes
      if (prev && openable && gc.isDoubleclick(c)) {
        c.clearDoubleclick();
        if (open) close();
        else open();
      }
      return true;
    }
    return super.mouseDown(x, y, c);
  }
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    if (ch.get(0).keyF(key, scancode, a)) return true;
    if (a.release) return false;
    
    
    if (key.k_left()) return left();
    if (key.k_up()) return up();
    
    
    if (key.k_right()) {
      if (!open && openable) {
        open();
        return true;
      }
    }
    if (key.k_right() || key.k_down()) return right();
    
    if (key.k_home()) {
      ctx.win().focus(base.ch.get(0));
      return true;
    }
    if (key.k_end()) {
      Node c = base;
      while (c instanceof ATreeNode && ((ATreeNode) c).open && c.ch.sz>1) c = c.ch.peek();
      ctx.win().focus(c);
    }
    
    return false;
  }
  
  
  // these return if focused element was changed
  public boolean up() {
    int i = p.ch.indexOf(this);
    if (i<=((ATreeNode) p).startN) {
      if (!(p instanceof TNNode)) return false;
      ctx.win().focus(p);
    } else {
      Node c = p.ch.get(i-1);
      while (c instanceof TNNode && ((TNNode) c).open && c.ch.sz>1) c = c.ch.peek();
      ctx.win().focus(c);
    }
    return true;
  }
  public boolean right() { // doesn't include opening self
    if (ch.sz>1 && open) {
      ctx.win().focus(ch.get(1));
      return true;
    }
    return down();
  }
  public boolean down() { // doesn't go through children; for that, use right()
    int i = p.ch.indexOf(this);
    if (i+1 < p.ch.sz) {
      ctx.win().focus(p.ch.get(i+1));
    } else {
      Node c = this;
      while (c instanceof TNNode && c.p.ch.peek()==c) c = c.p;
      if (!(c instanceof TNNode)) return false;
      ctx.win().focus(c.p.ch.get(c.p.ch.indexOf(c)+1));
    }
    return true;
  }
  public boolean left() {
    if (open) close();
    else if (p instanceof TNNode) ctx.win().focus(p);
    else { assert p instanceof TreeNode;
      int i = p.ch.indexOf(this);
      if (i==0) return false;
      ctx.win().focus(p.ch.get(i-1));
    }
    return true;
  }
}
