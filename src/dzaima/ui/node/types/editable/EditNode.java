package dzaima.ui.node.types.editable;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.undo.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;
import io.github.humbleui.skija.paragraph.*;

import java.util.Arrays;
import java.util.function.IntPredicate;

public class EditNode extends Node {
  public final Vec<Line> lns = new Vec<>();
  public final Vec<Pointer> ps = new Vec<>();
  public final Vec<Cursor> cs = new Vec<>(); // must always be sorted
  public final UndoManager um;
  public Font f;
  public long cursorTime;
  public final boolean multiline;
  public boolean wrap;
  
  public int bgSel, bgSelU;
  public int drawOffX, drawOffY;
  public int tsz;
  public boolean cursorWhenUnfocused;
  public EditNode(Ctx ctx, String[] ks, Prop[] vs, boolean multiline) { this(ctx, ks, vs, multiline, new UndoManager(ctx.gc)); }
  public EditNode(Ctx ctx, String[] ks, Prop[] vs, boolean multiline, UndoManager um) { // in case you want a more global undo/redo
    super(ctx, ks, vs);
    aTick();
    this.um = um;
    this.multiline = multiline;
    
    setFamily(vs[id("family")].str());
    
    lns.add(createLine(new char[0], 0));
    initCursor();
    
    int textId = id("text");
    int s = um.pushIgnore();
    if (textId>=0) insert(0, 0, vs[textId].str());
    um.popIgnore(s);
  }
  
  
  protected IntPredicate fn; // return whether enter was consumed
  public /*open*/ boolean enter(int mod) { if (fn!=null) return fn.test(mod); return false; }
  public void setFn(IntPredicate fn) { this.fn = fn; }
  
  
  public void propsUpd() { super.propsUpd();
    int ptsz = tsz;
    tsz = gc.len(this, "tsz", "textarea.tsz");
    if (tsz!=ptsz && f!=null) setFamily(f.tf.name);
    
    defTStyle.setFontSize(f.sz);
    defTStyle.setTypeface(f.tf.tf);
    defTStyle.setFontFamily(f.tf.name);
    defPStyle.setStrutStyle(new StrutStyle().setFontSize(1).setHeight(f.hi).setHeightOverridden(true).setHeightForced(true).setEnabled(true));
    bgSel  = gc.col(this, "bgSel" , "textarea.bgSel");
    bgSelU = gc.col(this, "bgSelU", "textarea.bgSelU");
    cursorWhenUnfocused = gc.boolD(this, "cursorWhenUnfocused", false);
    for (Line c : lns) { c.clearPara(); c.cw = -1; }
    pw = -1; ph = -1;
  }
  public Font font() {
    if ((flags&PROPS)!=0) propsUpd();
    return f;
  }
  public void setFamily(String family) {
    f = Typeface.of(family).sizeMode(tsz, 0);
  }
  
  public /*open*/ Line createLine(char[] p, int y) { return new Line(p, y); }
  protected /*open*/ void initCursor() { ps.add(cs.add(new Cursor(this))); } // not to be called manually!
  
  public /*open*/ Cursor addCursor(int p, Cursor c) {
    Cursor n = new Cursor(c);
    u(new Undo() {
      public void redo() { ps.add(cs.insert(p, n)); }
      public void undo() { cs.remove(n); ps.remove(n); }
    });
    return n;
  }
  public /*open*/ Cursor addCursor(int p, int sx, int sy, int ex, int ey) {
    Cursor n = new Cursor(this);
    n.sx = sx; n.sy = sy;
    n.ex = ex; n.ey = ey;
    u(new Undo() {
      public void redo() { ps.add(cs.insert(p, n)); }
      public void undo() { cs.remove(n); ps.remove(n); }
    });
    return n;
  }
  
  public /*open*/ Line ln(int y) { return lns.get(y); }
  
  public /*open*/ void modFrom(int y, int dy) {
    ph = -1;
    if (dy!=0) for (int i = y; i < lns.sz; i++) lns.get(i).y+= dy;
  }
  
  public /*open*/ void insert(int x, int y, String s) { // todo split into insertL/insertR? this is insertL
    int p = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i)=='\n') {
        insert(x, y, Tools.get(s, p, i));
        insert(x+i-p, y, '\n', false);
        p = i+1;
        x = 0;
        y++;
      }
    }
    insert(x, y, Tools.get(s, p, s.length()));
  }
  
  public /*open*/ void remove(int sx, int sy, int ex, int ey) {
    for (Pointer c : ps) c.collapse(sx, sy, ex, ey);
    Line sl = lns.get(sy);
    if (sy==ey) {
      sl.cut(sx, ex);
    } else {
      sl.cut(sx, sl.sz());
      Line el = lns.get(ey);
      sl.append(el.get(ex, el.sz()));
      Line[] r = lns.get(sy+1, ey+1, Line[].class);
      u(new Undo() {
        public void redo() {
          lns.remove(sy+1, ey+1);
          modFrom(sy+1, sy-ey);
        }
        public void undo() {
          modFrom(sy+1, ey-sy);
          lns.addAll(sy+1, r);
        }
      });
    }
  }
  public void removeAll() {
    um.pushL("remove all");
    remove(0, 0, ln(lns.sz-1).sz(), lns.sz-1);
    um.pop();
  }
  
  public /*open*/ void insert(int x, int y, char[] p) { // p won't contain newlines
    lns.get(y).insert(x, p);
    for (Pointer c : ps) c.dx(x, y, p.length);
  }
  public /*open*/ int insertU(int x, int y, int p, boolean human) {
    if (Character.charCount(p)==1) {
      insert(x, y, (char)p, human);
      return 1;
    } else {
      insert(x  , y, Character.highSurrogate(p), human);
      insert(x+1, y, Character. lowSurrogate(p), human);
      return 2;
    }
  }
  public /*open*/ void insert(int x, int y, char p, boolean human) { // p might be 10 aka '\n'
    Line l = lns.get(y);
    if (p==10) {
      if (!multiline) {
        if (human) enter(0);
        return;
      }
      char[] i = l.cut(x, l.sz());
      Line l2 = createLine(i, y+1);
      u(new Undo() {
        public void redo() {
          lns.insert(y+1, l2);
          modFrom(y+2, 1);
        }
        public void undo() {
          modFrom(y+2, -1);
          lns.removeAt(y+1);
        }
      });
      for (Pointer c : ps) c.ln(x, y);
    } else {
      l.insert(x, p);
      for (Pointer c : ps) c.dx(x, y, 1);
    }
  }
  public void append(String s) {
    um.pushQ("append");
    insert(lns.get(lns.sz-1).sz(), lns.sz-1, s);
    um.pop();
  }
  public /*open*/ XY find(int rx, int ry) {
    int yl, yo;
    int ryd = ry/f.hi;
    if (!wrap) {
      yl = Math.min(Math.max(ryd, 0), lns.sz-1);
      yo = yl*f.hi;
    } else {
      yo = 0;
      yl = 0;
      for (Line ln : lns) {
        yo+= ln.ch;
        if (yo > ryd) {
          yo = (yo-ln.ch) * f.hi;
          yl = ln.y;
          break;
        }
      }
    }
    Line l = ln(yl);
    l.buildPara();
    int dy = ry - yo;
    int pos = l.findX(rx, dy);
    return new XY(pos, yl);
  }
  
  public void sortCursors() { // TODO don't
    Cursor[] sorted = cs.toArray(new Cursor[0]);
    Arrays.sort(sorted);
    Vec<Cursor> ncs = new Vec<>(sorted.length);
    Cursor p = ncs.add(sorted[0]);
    for (int i = 1; i < sorted.length; i++) {
      Cursor c = sorted[i];
      if (!p.equals(c)) p = ncs.add(c);
    }
    if (!ncs.equals(cs)) u(new Undo() {
      public void redo() { cs.swap(ncs); }
      public void undo() { cs.swap(ncs); }
    });
  }
  
  public void drawC(Graphics g) {
    g.push();
    g.translate(drawOffX, drawOffY);
    getSize();
    g.clip(0, 0, w-drawOffX, h-drawOffY);
    CIt cit = new CIt(cs);
    int y = 0;
    for (Line ln : lns) {
      int lh = f.hi*ln.ch;
      if (g.clip==null || y+lh > g.clip.sy && y < g.clip.ey) ln.draw(g, y, cit);
      else ln.notDrawn();
      
      y+= lh;
    }
    if (drawCursor) for (Cursor c : cs) c.draw(g);
    g.pop();
  }
  
  protected int pw = -1, ph = -1; // previously gotten width/height; cached, may not be 100% accurate, may also be -1 if getSize() wasn't called
  private void getSize() {
    if (ph!=-1 && pw!=-1) return;
    int nw = 0;
    int nh = 0;
    for (Line c : lns) {
      nw = Math.max(nw, (int) c.width());
      c.yw = nh;
      nh+= c.ch;
    }
    nh*= f.hi;
    nh+= drawOffY;
    nw+= drawOffX+1; // TODO cursor width
    pw = nw;
    ph = nh;
    mResize();
  }
  
  public int minW() { if(wrap) return f.hi*3; getSize(); return pw; }
  public int minH(int w) { tmpW(w); getSize(); rstW(); return multiline? ph            : f.hi; }
  public int maxH(int w) { tmpW(w); getSize(); rstW(); return multiline? super.maxH(w) : f.hi; }
  private int aw;
  private void tmpW(int nw) { if (!wrap) return;
    if (nw!=w) for (Line l : lns) l.clearPara();
    aw = w;
    w = nw;
  }
  private void rstW() { if (!wrap) return;
    w = aw;
  }
  
  
  public <T> T u(UndoR<T> u) {
    return um.u(u);
  }
  
  
  public boolean isFocused;
  public boolean drawCursor;
  public /*open*/ void tickC() {
    // TODO call some gc.requestTickIn(n milliseconds) to make sure that the cursor properly flashes
    long dt = gc.lastMs-cursorTime;
    if (dt > gc.cursorOnMs*4) cursorTime = gc.lastMs;
    else if (dt > gc.cursorOnMs*2) cursorTime+= gc.cursorOnMs*2;
    boolean drawCursorC = isFocused? (dt<gc.cursorOnMs || !ctx.win().focused) : cursorWhenUnfocused;
    if (drawCursorC != drawCursor) {
      drawCursor = drawCursorC;
      mRedraw();
    }
  }
  
  public void focusS() {
    super.focusS();
    isFocused = true;
  }
  
  public void focusE() {
    super.focusE();
    isFocused = false;
  }
  
  public void hoverS() { ctx.win().setCursor(Window.CursorType.IBEAM); }
  public void hoverE() { ctx.win().setCursor(Window.CursorType.REGULAR); }
  
  public boolean mouseDown(int x, int y, Click c) { // TODO mobile-friendly
    x-= drawOffX;
    y-= drawOffY;
    if (c.btn!=Click.LEFT) return false;
    ctx.win().focus(this);
    XY l = find(x, y);
    if (Key.only(c.mod, Key.M_ALT)) {
      um.pushU("add cursor");
      addCursor(cs.sz, cs.peek()).mv(l.x, l.y);
      sortCursors();
      um.pop();
    } else {
      um.pushU("move cursor mouse");
      collapseCursors(true);
      if (!Key.shift(c.mod)) cs.get(0).mv(l.x, l.y);
      um.pop();
      c.notify(this, x, y);
    }
    mRedraw();
    return true;
  }
  public void mouseTick(int x, int y, Click c) {
    XY l = find(x, y);
    um.pushU("move cursor mouse");
    cs.get(0).mv(true, l.x, l.y);
    um.pop();
    mRedraw();
  }
  
  protected void collapseCursors(boolean first) {
    if (cs.sz>1) {
      um.pushU("collapse cursors");
      u(new Undo() {
        Cursor[] t;
        public void redo() {
          mRedraw();
          final int s = first? 1 : 0;
          int e = first? cs.sz : cs.sz-1;
          t = cs.get(s, e, Cursor[].class);
          for (Cursor c : t) ps.remove(c);
          cs.remove(s, e);
        }
        public void undo() {
          if (first) { cs.addAll(cs.sz, t); ps.addAll(ps.sz, t); }
          else       { cs.addAll(0,     t); ps.addAll(0,     t); }
          mRedraw();
        }
      });
      um.pop();
    }
  }
  protected void removeCursor(int pos) {
    assert cs.sz>1;
    um.pushU("remove cursor");
    u(new Undo() {
      Cursor[] t;
      public void redo() {
        mRedraw();
        t = cs.get(1, cs.sz, Cursor[].class);
        for (Cursor c : t) ps.remove(c);
        cs.remove(1, cs.sz);
      }
      public void undo() { cs.addAll(cs.sz, t); ps.addAll(ps.sz, t); mRedraw(); }
    });
    um.pop();
  }
  public void replaceCursors(Vec<Cursor> newCursors) {
    um.pushU("replace cursors");
    u(new Undo() {
      private final Cursor[] prevCursors = cs.toArray(new Cursor[0]);
      public void redo() {
        mRedraw();
        for (Cursor c : cs) ps.remove(c);
        cs.clear();
        cs.addAll(0, newCursors);
        ps.addAll(0, newCursors);
      }
      public void undo() {
        for (Cursor c : cs) ps.remove(c);
        cs.clear();
        cs.addAll(0, prevCursors);
        ps.addAll(0, prevCursors);
        mRedraw();
      }
    });
    um.pop();
  }
  
  public String get(int x0, int y0, int x1, int y1) { // incl x0,y0,y1, excl x1
    StringBuilder b = new StringBuilder();
    if (y0==y1) {
      b.append(ln(y0).get(x0, x1));
    } else {
      Line l0 = ln(y0);
      b.append(l0.get(x0, l0.sz()));
      b.append('\n');
      for (int y = y0+1; y <= y1-1; y++) {
        Line l = ln(y);
        b.append(l.get(0, l.sz()));
        b.append('\n');
      }
      b.append(ln(y1).get(0, x1));
    }
    return b.toString();
  }
  public String getAll() {
    return get(0, 0, ln(lns.sz-1).sz(), lns.sz-1);
  }
  public String getByCursor(Cursor c) {
    return get(c.xm(), c.ym(), c.xM(), c.yM());
  }
  public void replaceByCursor(Cursor c, String val) {
    c.order();
    int sx = c.sx;
    int sy = c.sy;
    remove(sx, sy, c.ex, c.ey);
    insert(sx, sy, val);
    c.mv(sx, sy, c.ex, c.ey);
  }
  
  public void typed(int codepoint) {
    um.pushQ("type");
    for (Cursor c : cs) c.typed(codepoint);
    um.pop();
  }
  
  
  public boolean anySel() {
    for (Cursor c : cs) if (c.sel()) return true;
    return false;
  }
  public boolean allSel() {
    for (Cursor c : cs) if (!c.sel()) return false;
    return true;
  }
  
  public /*open*/ boolean copy() { // returns selection was changed
    StringBuilder b = new StringBuilder();
    boolean sel = anySel();
    if (sel) {
      for (Cursor c : cs) b.append(getByCursor(c)).append('\n');
      b.deleteCharAt(b.length()-1);
    } else {
      um.pushU("copy line");
      for (Cursor c : cs) {
        Line l = ln(c.sy);
        b.append(l.get()).append('\n');
        c.mv(l.sz(), c.sy, 0, c.sy);
      }
      um.pop();
    }
    ctx.win().copyString(b.toString());
    return !sel;
  }
  
  public void pasteText(String s) {
    if (s==null) return;
    String[] lns = Tools.split(s, '\n');
    if (lns.length==cs.sz || cs.sz>1 && lns.length==cs.sz+1 && lns[lns.length-1].isEmpty()) {
      for (int i = 0; i < cs.sz; i++) {
        Cursor c = cs.get(i);
        c.clearSel();
        insert(c.sx, c.sy, lns[i]);
      }
    } else {
      for (Cursor c : cs) {
        c.clearSel();
        insert(c.sx, c.sy, s);
      }
    }
  }
  public void paste() {
    ctx.win().pasteString(this::pasteText);
  }
  
  public static boolean isName(char c) {
    return Character.isUnicodeIdentifierPart(c);
  }
  
  int idxOf(String s, Line ln, int sx, int ex) {
    int i = new String(Arrays.copyOfRange(ln.a.arr, sx, ex)).indexOf(s);
    return i==-1? -1 : sx+i;
  }
  
  public XY searchString(String s, int sx, int sy, int ex, int ey) { // incl ey, excl ex
    if (s.contains("\n")) {
      System.out.println("TODO multiline search");
      return null;
    }
    if (sy==ey) {
      int i = idxOf(s, ln(sy), sx, ex);
      return i==-1? null : new XY(i, sy);
    }
    int rS = idxOf(s, ln(sy), sx, ln(sy).sz());
    if (rS!=-1) return new XY(rS, sy);
    for (int y = sy+1; y <= ey-1; y++) {
      int r = idxOf(s, ln(y), 0, ln(y).sz());
      if (r!=-1) return new XY(r, y);
    }
    int rE = idxOf(s, ln(ey), 0, ex);
    if (rE!=-1) return new XY(rE, ey);
    return null;
  }
  
  
  public int action(Key key, KeyAction a) { // 0-unused; 1-used; 2-won't use
    switch (gc.keymap(key, a, "textarea")) {
      case "copy": copy(); return 1;
      case "paste": um.pushQ("paste"); paste(); um.pop(); return 1;
      case "cut":
        um.pushQ("cut");
        boolean b = copy();
        for (Cursor c : cs) c.clearSel();
        if (b) for (Cursor c : cs) c.delR(0);
        sortCursors();
        um.pop();
        return 1;
      case "selectAll":
        um.pushU("select all");
        collapseCursors(true);
        cs.get(0).mv(0,0, ln(lns.sz-1).sz(), lns.sz-1);
        um.pop();
        return 1;
      case "undo": um.undo(); return 1;
      case "redo": um.redo(); return 1;
      case "keepFirst":
        if (cs.sz > 1) { collapseCursors(true); return 1; }
        if (cs.peek().sel()) { um.pushQ("keep first"); cs.peek().mv(cs.peek().ex, cs.peek().ey); um.pop(); return 1; }
        return 2;
      case "keepLast":
        if (cs.sz > 1) { collapseCursors(false); return 1; }
        if (cs.peek().sel()) { um.pushQ("keep last"); cs.peek().mv(cs.peek().sx, cs.peek().sy); um.pop(); return 1; }
        return 2;
      default: return 0;
    }
  }
  
  public final boolean keyF(Key key, int scancode, KeyAction a) {
    int ar = action(key, a);
    if (ar!=0) return ar==1;
    return keyF2(key, scancode, a);
  }
  
  public boolean keyF2(Key key, int scancode, KeyAction a) { // TODO keymap
    if (a.release) return false;
    
    if (key.k_enter()) {
      if (enter(key.mod)) return true;
      if (!multiline) return true;
      um.pushQ("new line"); for (Cursor c : cs) c.typed(10); sortCursors(); um.pop();
    } else if ((key.k_backspace() || key.k_del()) && anySel()) {
      um.pushQ("delete selection"); for (Cursor c : cs) c.clearSel(); sortCursors(); um.pop();
    }
    else if (key.k_backspace()) { um.pushQ("delete char"); for (Cursor c : cs) c.delL (key.mod); sortCursors(); um.pop(); }
    else if (key.k_del      ()) { um.pushQ("delete char"); for (Cursor c : cs) c.delR (key.mod); sortCursors(); um.pop(); }
    else if (key.k_left     ()) { um.pushU("move cursor"); for (Cursor c : cs) c.left (key.mod); sortCursors(); um.pop(); }
    else if (key.k_right    ()) { um.pushU("move cursor"); for (Cursor c : cs) c.right(key.mod); sortCursors(); um.pop(); }
    else if (key.k_up       ()) { um.pushU("move cursor"); for (Cursor c : cs) c.up   (key.mod); sortCursors(); um.pop(); }
    else if (key.k_down     ()) { um.pushU("move cursor"); for (Cursor c : cs) c.down (key.mod); sortCursors(); um.pop(); }
    else if (key.k_end      ()) { um.pushU(key.hasCtrl()?"move cursor far":"move cursor"); for (Cursor c : cs) c.end (key.mod); sortCursors(); um.pop(); }
    else if (key.k_home     ()) { um.pushU(key.hasCtrl()?"move cursor far":"move cursor"); for (Cursor c : cs) c.home(key.mod); sortCursors(); um.pop(); }
    return true;
  }
  
  public class Line {
    public int y; // line number
    public int yw; // physical line number, can be greater than y if wrapping is on 
    public ChrVec a = new ChrVec();
    public ByteVec st = new ByteVec();
    
    public Line(char[] p, int y) {
      this.y = y;
      init(p);
    }
    public /*open*/ void init(char[] p) { insertA(0, p); } // stupid java and its insistence on super being the first call in constructors
    
    // read primitives
    public /*open*/ int sz() { return a.sz; }
    public /*open*/ char[] get(int s, int e) { return a.get(s, e); }
    public /*open*/ char get(int i) { return a.get(i); }
    public /*open*/ String get() { return new String(get(0, sz())); }
    
    // write primitives
    protected /*open*/ void insertA(int x, char[] p          ) { mod(); a.addAll(x, p); st.insertFill(x, p.length, x<1? 0 : st.get(x-1)); }
    protected /*open*/ void insertA(int x, char[] p, byte[] s) { mod(); a.addAll(x, p); st.addAll(x, s); }
    protected /*open*/ void insertA(int x, char p)             { mod(); a.insert(x, p); st.insert    (x,           x<1? 0 : st.get(x-1)); }
    protected /*open*/ void rmA    (int s, int e)              { mod(); a.remove(s, e); st.remove(s, e); }
    
    protected /*open*/ void mod() {
      clearPara();
      mRedraw();
      cw = -1;
      ph = -1;
    }
    public void clearPara() {
      if (bp!=null) bp.close(); bp = null;
    }
    
    
    public float width() {
      if (cw==-1) buildPara();
      return cw;
    }
    public XY real(int x) { // character index to screen pos
      if (bp==null) return XY.ZERO;
      char c = x<1|x>sz()? 0 : get(x-1);
      int x0 = x-1, x1 = x;
      boolean high = false;
      if (Character.isSurrogate(c)) {
        high = Character.isHighSurrogate(c);
        if (high) x1++;
        else x0--;
      }
      TextBox[] rs = bp.getRectsForRange(x0, x1, RectHeightMode.MAX, RectWidthMode.TIGHT);
      if (rs.length == 0) return XY.ZERO;
      io.github.humbleui.types.Rect r = rs[0].getRect();
      int rx;
      if (!high) rx = (int) r.getRight();
      else rx = (int) (r.getLeft()+r.getRight())/2;
      return new XY(rx, (int)r.getTop());
    }
    public XY real(int x, boolean right) { // return position of either the left or right side of the char starting at position x
      if (bp==null) return XY.ZERO;
      int x0 = x, x1 = x+1;
      char c = get(x0);
      if (Character.isSurrogate(c)) {
        if (Character.isHighSurrogate(c)) x1++;
        else x0--;
      }
      TextBox[] rs = bp.getRectsForRange(x0, x1, RectHeightMode.MAX, RectWidthMode.TIGHT);
      if (rs.length!=1) {
        System.err.println("No bounding box for character found: "+(int)c);
        return XY.ZERO;
      }
      io.github.humbleui.types.Rect r = rs[0].getRect();
      int rx = (int) (right? r.getRight() : r.getLeft());
      return new XY(rx, (int)r.getTop());
    }
    public int findX(int rx, int dy) {
      buildPara();
      return bp.getGlyphPositionAtCoordinate(rx, dy).getPosition();
    }
    public int leadingWs() {
      int r = 0;
      while (r<sz() && Tools.isWs(get(r))) r++;
      return r;
    }
    
    
    // undoable stuff
    public void append(char[] p          ) { insert(sz(), p   ); }
    public void append(char[] p, byte[] s) { insert(sz(), p, s); }
    public void insert(int x, char[] p) {
      u(new Undo() {
        public void redo() { insertA(x, p); }
        public void undo() { rmA(x, x+p.length); }
      });
    }
    public void insert(int x, char[] p, byte[] s) {
      u(new Undo() {
        public void redo() { insertA(x, p, s); }
        public void undo() { rmA(x, x+p.length); }
      });
    }
    public void insert(int x, char p) {
      u(new Undo() {
        public void redo() { insertA(x, p); }
        public void undo() { rmA(x, x+1); }
      });
    }
    
    public char[] cut(int x0, int x1) {
      return u(new UndoR<char[]>() {
        char[] cut;
        public char[] redoR() { cut = get(x0, x1); rmA(x0, x1); return cut; }
        public void undo() { insertA(x0, cut); }
      });
    }
    
    // draw stuff
    public int cw=-1, ch=1; // cached width in pixels, height in lines; ch is always positive, cw can be -1
    private Paragraph bp;
    public void buildPara() {
      if (bp!=null) return;
      ParagraphBuilder b = new ParagraphBuilder(defPStyle, Typeface.fontCol);
      b.pushStyle(style((byte) 0));
      byte ps = 0;
      int pi = 0;
      int sz = sz();
      for (int i = 0; i < sz; i++) {
        byte cs = st.get(i);
        if (cs!=-1 && cs!=ps) {
          b.addText(new String(get(pi, i)));
          b.popStyle();
          b.pushStyle(style(cs));
          ps = cs;
          pi = i;
        }
      }
      if (pi!=sz) b.addText(new String(get(pi, sz)));
      bp = b.build();
      b.close();
      bp.layout(wrap? EditNode.this.w-drawOffX : Float.POSITIVE_INFINITY);
      
      LineMetrics[] m = bp.getLineMetrics();
      int nh = Math.max(1, m.length);
      cw = 0;
      for (LineMetrics c : m) {
        int e = (int) c.getEndIndex();
        if (e>0) cw = Math.max(cw, real(e-1, true).x);
      }
      
      if (nh!=ch) { ch=nh; ph = -1; }
    }
    public TextStyle style(byte b) { return defTStyle; }
    
    public void selectedBg(Graphics g, int gy, CIt cit) {
      int bg = isFocused? bgSel : bgSelU;
      cit.on(y); Cursor c;
      while ((c = cit.next()) != null) {
        if (c.reg()) continue;
        if (ch>1) {
          LineMetrics[] lm = bp.getLineMetrics();
          int si = c.ym()==y? c.xm() : 0;
          int ei = c.yM()==y? c.xM() : (int) lm[lm.length-1].getEndIndex();
          for (LineMetrics m : lm) {
            int ms = (int) m.getStartIndex();
            int me = (int) m.getEndIndex();
            if (si>=me || ei<=ms) continue;
            int sx = si>=ms? real(si,  false).x : 0;
            int ex = ei<=me? real(ei-1, true).x : real(me-1, true).x;
            int my = (int) (m.getBaseline() - m.getAscent());
            g.rect(sx, gy+(my<2?0:my), ex, gy+f.hi+my, bg);
          }
        } else {
          int sx = c.ym()==y? real(c.xm()).x : 0;
          int ex = c.yM()==y? real(c.xM()).x : (int) width();
          g.rect(sx, gy, ex, gy+f.hi, bg);
        }
      }
    }
    public /*open*/ void draw(Graphics g, int gy, CIt cit) {
      assert ln(y)==this;
      buildPara();
      selectedBg(g, gy, cit);
      bp.paint(g.canvas, 0, gy);
    }
    public /*open*/ void notDrawn() { clearPara(); }
  }
  private final ParagraphStyle defPStyle = new ParagraphStyle();
  private final TextStyle      defTStyle = new TextStyle();
  
  
  
  public static class CIt { // cursor iterator
    Vec<Cursor> cs;
    public int i;
    int cy;
    public CIt(Vec<Cursor> cs) {
      this.cs = cs;
    }
    public void on(int y) {
      cy = y;
      while (i<cs.sz && cs.get(i).yM()<y) i++;
    }
    public Cursor next() {
      if (i>=cs.sz || cy==-1) return null;
      Cursor c = cs.get(i);
      if (c.ym() > cy) return null;
      if (c.yM() > cy) cy = -1;
      else i++;
      return c;
    }
  }
}
