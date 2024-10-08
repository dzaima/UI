package dzaima.ui.node;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.ScrollNode;
import dzaima.utils.*;

import java.util.function.*;

public abstract class Node implements Click.RequestImpl {
  public final Ctx ctx;
  public final GConfig gc;
  public Props props;
  public final Vec<Node> ch;
  public short flags = RD_ME|RD_CH|RS_ME|RS_CH|PROPS|ANYCT|MTICK;
  @SuppressWarnings("PointlessBitwiseExpression") // ugh
  public static final short RD_ME = 1<<0; // redraw this; if present, RD_CH must also be
  public static final short RD_AL = 1<<1; // always redraw this when redrawing any children
  public static final short RD_CH = 1<<2; // some children need redrawing
  public static final short RS_ME = 1<<3; // resize this; if present, RS_CH must also be
  public static final short RS_CH = 1<<4; // resize children
  public static final short PROPS = 1<<5; // whether props have been modified
  public static final short ATICK = 1<<6; // whether this always needs tick
  public static final short MTICK = 1<<7; // whether this needs tick on the next frame
  public static final short ANYCT = 1<<8; // whether any child has PROPS/ATICK/MTICK
  public static final short F_C1 = 1<<14;
  public static final short F_C2 = (short) (1<<15);
  
  public Node p;
  public boolean visible = false;
  
  public Node(Ctx ctx, Props props) {
    this.ctx = ctx;
    this.gc = ctx.gc;
    this.props = props;
    this.ch = new Vec<>(); // TODO maybe have a global empty Vec such that this isn't a wasted allocation?
  }
  
  
  ///////// visibility, node modifications \\\\\\\\\
  public void add(Node n) {
    assert !n.visible;
    n.p = this;
    basicPos(n, ch.sz-1);
    ch.add(n);
    if (visible) { n.shown(); mMod(); }
  }
  public int remove(Node n) {
    int r = ch.remove(n);
    if (visible) { n.hidden(); mMod(); }
    return r;
  }
  public void remove(int s, int e) {
    if (s==e) return;
    assert s>=0 && s<ch.sz : "Couldn't remove elements "+s+" to "+e+" in "+Devtools.debugMe(this);
    if (visible) for (int i = s; i < e; i++) ch.get(i).hidden();
    ch.remove(s, e);
    if (visible) mMod();
  }
  public void clearCh() {
    if (visible) for (Node c : ch) c.hidden();
    ch.clear();
    if (visible) mMod();
  }
  public void insert(int i, Vec<Node> nds) {
    assert i>=0 && i<=ch.sz : "Couldn't insert element at "+i+" in "+Devtools.debugMe(this);
    for (Node n : nds) {
      assert !n.visible;
      n.p = this;
      basicPos(n, i-1);
      if (visible) n.shown();
    }
    ch.insert(i, nds);
    if (visible) mMod();
  }
  public void insert(int i, Node n) {
    assert !n.visible;
    n.p = this;
    if (visible) n.shown();
    basicPos(n, i-1);
    ch.insert(i, n);
    if (visible) mMod();
  }
  public void replace(int i, Node n) {
    Node p = ch.get(i);
    if (visible) p.hidden();
    n.dx = p.dx; n.dy = p.dy; // basicPos replacement
    ch.set(i, n);
    n.p = this;
    if (visible) { n.shown(); mMod(); }
  }
  public void replace(int i, Function<Node, Node> f) {
    Node p = ch.get(i);
    if (visible) p.hidden();
    Node n = f.apply(p);
    n.dx = p.dx; n.dy = p.dy; // basicPos replacement
    ch.set(i, n);
    n.p = this;
    if (visible) { n.shown(); mMod(); }
  }
  public int replaceSelfInParent(Supplier<Node> f) { // returns affected index in parent
    int i = p.ch.indexOf(this);
    p.replace(i, n -> f.get());
    return i;
  }
  public void swap(int i, int j) {
    if (i==j) return;
    Node ci = ch.get(i); int ix=ci.dx, iy=ci.dy;
    Node cj = ch.get(j); int jx=cj.dx, jy=cj.dy;
    ch.set(j, ci); ci.dx=jx; ci.dy=jy;
    ch.set(i, cj); cj.dx=ix; cj.dy=iy;
    if (visible) mMod();
  }
  private void mMod() {
    assert visible;
    mResize();
    if (!Tools.DBG) return;
    Devtools d = Devtools.getDevtools(this);
    if (d!=null) d.modified(this);
  }
  private void basicPos(Node n, int i) { // make binary searches over just modified nodes not _completely_ break
    if (ch.sz>0) {
      Node p = ch.get(Math.min(Math.max(i, 0), ch.sz));
      n.dx = p.dx;
      n.dy = p.dy;
    } else n.dx = n.dy = 0;
  }
  
  public void shown() {
    assert !visible : getClass()+" shown twice!";
    visible = true;
    hProps(); // this was previously under a (flags&PROPS)!=0 but that doesn't work when a node is removed from the tree, global em changed, and readded
    for (Node c : ch) c.shown();
    if ((flags&ATICK)!=0 && p!=null) aTick();
    if ((flags&MTICK)!=0 && p!=null) mTick();
  }
  public void hidden() {
    assert visible : getClass()+" hidden while hidden!";
    visible = false;
    for (Node c : ch) c.hidden();
    mResize();
  }
  private void hProps() { propsUpd(); flags&=~PROPS; }
  
  
  
  ///////// properties \\\\\\\\\
  public static final String[] KS_NONE = new String[0];
  public static final Prop  [] VS_NONE = new Prop[0];
  
  public final Prop getProp(String name) {
    return props.get(name);
  }
  public final Prop getPropN(String name) {
    return props.getNullable(name);
  }
  public final void setProp(String name, Prop p) {
    props = props.with(name, p);
    mProp();
  }
  public final boolean hasProp(String name) {
    return props.has(name);
  }
  
  public /*open*/ void propsUpd() { mResize(); } // probably rare enough. overriders can choose to not call anyways
  
  
  
  ///////// per-frame stuff \\\\\\\\\
  /*
    sequence of events within a frame:
      1. global resize if window size changed
      2. events
        1. general events in unknown order
        2. Node::mouseTick
        3. Window::eventTick (incl. hoverS/hoverE)
      3. maybe resize (incl. propsUpd if ran)
      4. tick (incl. propsUpd)
      5. maybe resize (incl. propsUpd if ran; yes, resize is called twice per frame, to guarantee that before both tick and draw there's proper layouting; in most cases only one will be triggered, but that's not guaranteed)
      6. draw
    
    Override `drawC` with a function that draws foreground elements of this only
      Children drawing is done with drawCh, called separately
    Override `bg` with a function that draws the background of this only.
      But invoke `pbg(g)` at the start if what you draw might be transparent (and thus require the parent bg to be drawn)
    
    Draw order:
      if only children need to be redrawn:
        1. draw children with drawCh(g, full)
        2. draw overlapping things with over(g)
      if myself or parents need to be redrawn:
        1. draw own background with bg(g, full)
        2. draw myself with drawC(g)
        3. draw children with drawCh(g, full)
        4. draw overlapping things with over(g)
    
    Resize stuff:
      `mResize()` includes redrawing itself, but not parents
      if your draw functions won't necessarily completely redraw your full canvas space, add mRedraw() into your `resized`
        (that's probably only needed if your used canvas space changes between frames, but that's complicated and may not be correct)
   */
  public int w=-1, h=-1, dx=0, dy=0;
  public /*open*/ void bg(Graphics g, boolean full) { pbg(g, full); } // draw background; by default, fall back to parent; TODO this assumes clipping
  public /*open*/ void drawC(Graphics g) { } // draw self foreground
  public /*open*/ void drawCh(Graphics g, boolean full) {
    for (Node n : ch) { n.draw(g, full); assert n.p==this : "No parent set for "+ch.indexOf(n)+" in "+Devtools.debugMe(this); }
  }
  public /*open*/ void tickC() { } // anyone overriding this should also call aTick() in its constructor (or carefully use mTick())
  public /*open*/ void over(Graphics g) { }
  
  public final void draw(Graphics g, boolean full) { // called only by NodeWindow & drawCh; full only matters when !g.redraw
    assert visible : "Node "+Devtools.debugMe(this)+" not visible during draw";
    boolean meR = (flags&(RD_ME|RD_AL))!=0 || full || g.redraw;
    boolean chR = (flags&RD_CH)!=0 || meR;
    if (!chR) return;
    flags&= ~(RD_ME|RD_CH);
    g.push();
    g.translate(dx, dy);
    if (meR) {
      bg(g, full);
      drawC(g);
    }
    drawCh(g, meR);
    
    over(g);
    g.pop();
  }
  public final void pbg(Graphics g, boolean full) {
    if (!g.redraw && p!=null && !full) {
      g.push();
      g.clip(0, 0, w, h);
      g.translate(-dx, -dy);
      p.bg(g, false);
      g.pop();
    }
  }
  public final void tick() {
    int f0 = flags;
    if ((f0&(ATICK|MTICK|PROPS|ANYCT)) == 0) return;
    flags&= ~(MTICK|PROPS|ANYCT);
    if ((f0&PROPS) != 0) hProps();
    if ((f0&(ATICK|MTICK)) != 0) {
      if ((f0&ATICK) != 0) anyc();
      tickC();
    }
    if ((f0&ANYCT) != 0) for (Node c : ch) c.tick();
  }
  public final void propsTick() {
    int f0 = flags;
    if ((f0&PROPS) == 0) return;
    flags&= ~PROPS;
    hProps();
    if ((f0&ANYCT) != 0) for (Node c : ch) c.propsTick();
  }
  public final void aTick() { flags|= ATICK; anyc(); }
  public final void mTick() { flags|= MTICK; anyc(); }
  public final void mProp() { flags|= PROPS; anyc(); }
  private void anyc() {
    Node c = p;
    while (c!=null && (c.flags&ANYCT)==0) { c.flags|= ANYCT; c = c.p; }
  }
  
  
  
  ///////// redraw stuff \\\\\\\\\
  // any resize(w,h) calls must be between (minW(); minH(w)) and (maxW(); maxH(w)), and any such call must be accepted by the node; TODO max might be made or might already be non-obligatory
  public final void resize(int w, int h, int dx, int dy) {
    boolean sameSz = w==this.w && h==this.h;
    boolean samePos = this.dx==dx && this.dy==dy;
    boolean rsCH = (flags&RS_CH)!=0 || !sameSz;
    boolean rsME = (flags&RS_ME)!=0 || !sameSz || !samePos;
    
    if (!rsCH && samePos) return;
    if (rsME) mRedraw();
    this.dx = dx; this.w = w;
    this.dy = dy; this.h = h;
    resized();
    flags&= ~(RS_ME|RS_CH);
    assert minW( )<=w : "Requested width " +w+" less than the minimum "+minW( )+" for "+Devtools.debugMe(this);
    assert minH(w)<=h : "Requested height "+h+" less than the minimum "+minH(w)+" for "+Devtools.debugMe(this)+" (for w="+w+")";
  }
  protected /*open*/ void resized() { assert ch.sz==0 : "No resized() for "+Devtools.debugMe(this)+" (doesn't expect children)"; } // called when w/h have been updated; should resize children if applicable
  
  // minH and maxH should each give the same result for all w >= maxW()
  public /*open*/ int minW() { return 0; }
  public /*open*/ int minH(int w) { return 0; }
  public /*open*/ int maxW() { return Tools.BIG; }
  public /*open*/ int maxH(int w) { return Tools.BIG; }
  
  public final void mRedraw() {
    if ((flags&RD_ME)!=0) return;
    flags|= RD_ME;
    Node c = p;
    while (c!=null && (c.flags&RD_CH)==0) { c.flags|= RD_CH; c = c.p; }
  }
  public boolean needsRedraw() { return (flags&(RD_ME|RD_CH)) != 0; } // check whether this node needs redrawing
  public void mResize() { // includes mRedraw
    flags|= RS_ME|RS_CH;
    mChResize();
  }
  protected /*open*/ void mChResize() {
    // no early exit because that breaks if mResize is overridden for caching: 1. mResize(); 2. min/max W/H caching result; 3. children changed again; 4. calling mResize again, but not notifying parent to clear cache
    flags|= RS_CH;
    if (p!=null) p.mChResize();
  }
  public boolean needsResize() { return (flags&RS_CH) != 0; } // check whether this node doesn't know its size
  public /*open*/ XY relPos(Node sp) { // pass null to get position relative to base
    if (p==sp) return new XY(dx, dy);
    if (dx==0 && dy==0) return p.relPos(sp);
    return p.relPos(sp).add(dx, dy);
  }
  
  
  ///////// events \\\\\\\\\
  public /*open*/ Node findCh(int x, int y) { // return child that contains (x;y), else null; open for optimization or custom child deltas
    for (Node c : ch) if (XY.inWH(x, y, c.dx, c.dy, c.w, c.h)) return c;
    return null;
  }
  public /*open*/ Node nearestCh(int x, int y) {
    if (ch.sz<2) return ch.sz==0 || ch.get(0).w==-1? null : ch.get(0);
    int min = Integer.MAX_VALUE, curr;
    Node best = null;
    for (Node c : ch) if (c.w!=-1 && (curr=XY.dist(x, y, c.dx, c.dy, c.w, c.h))<min) {
      if (curr == 0) return c;
      min = curr;
      best = c;
    }
    return best;
  }
  public /*open*/ void mouseStart(int x, int y, Click c) {
    Node ch = findCh(x, y);
    if (ch!=null) ch.mouseStart(x-ch.dx, y-ch.dy, c);
  }
  public /*open*/ void mouseDown(int x, int y, Click c) { }
  public /*open*/ void mouseTick(int x, int y, Click c) { }
  public /*open*/ void mouseUp(int x, int y, Click c) { }
  public final GConfig gc() { return gc; }
  
  public /*open*/ boolean scroll(int x, int y, float dx, float dy) {
    Node c = findCh(x, y);
    if (c!=null) return c.scroll(x-c.dx, y-c.dy, dx, dy);
    return false;
  }
  public /*open*/ void hoverS() { }
  public /*open*/ void hoverE() { }
  public /*open*/ void hoverT(int mx, int my) { }
  
  // focusS & focusE may be called at any point in time! (except between resizing & drawing)
  public /*open*/ void focusS() { mRedraw(); ScrollNode.scrollTo(this, ScrollNode.Mode.FULLY_OFFSCREEN, ScrollNode.Mode.FULLY_OFFSCREEN); }
  // focusE will be called before a new node in this window gains focus regardless of whether this is visible
  // if this node is removed, and re-added before another node is focused, focus will be kept on this node  
  public /*open*/ void focusE() { mRedraw(); }
  public final void focusMe() { ctx.focus(this); }
  public boolean isFocused() { return ctx.win().focusNode()==this; }
  
  public /*open*/ void typed(int codepoint) { }
  public /*open*/ boolean keyF(Key key, int scancode, KeyAction a) { return false; } // for the focused element
  public /*open*/ boolean key(int x, int y, Key key, int scancode, KeyAction a) {
    Node c = findCh(x, y);
    if (c!=null) return c.key(x-c.dx, y-c.dy, key, scancode, a);
    return false;
  }
  
  public Vec<Prop> getProps() {
    return props.values();
  }
  public final Vec<Prop> addProps(Vec<Prop> prev, String... names) {
    for (String n : names) if (n!=null) prev.add(gc.getCfgProp(n));
    return prev;
  }
}
