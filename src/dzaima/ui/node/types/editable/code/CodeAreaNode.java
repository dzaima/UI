package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.undo.UndoManager;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.editable.*;
import dzaima.utils.*;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public class CodeAreaNode extends EditNode {
  public Language lang;
  private Lang langInst;
  public int padLns, padChrs; // TODO negative numbers add screen height (so -padLns are visible when scrolled all the way down)
  
  public boolean lineNumbering;
  int lnBg, lnCol;
  public CodeAreaNode(Ctx ctx, String[] ks, Prop[] vs) { this(ctx, ks, vs, true, new UndoManager(ctx.gc)); }
  public CodeAreaNode(Ctx ctx, String[] ks, Prop[] vs, boolean multiline, UndoManager um) {
    super(ctx, ks, vs, multiline, um);
    setLang(gc.langs().defLang);
    lineNumbering = true;
  }
  
  
  public String customWrapPairs;
  private String defWrapPairs;
  int lnL, lnR;
  public void propsUpd() { super.propsUpd();
    padLns = gc.intD(this, "padLns", padLns);
    padChrs = gc.intD(this, "padChrs", padChrs);
    lnCol = gc.col(this, "lnCol", "codearea.lnCol");
    lnBg = gc.col(this, "lnBg", "codearea.lnBg");
    lineNumbering = gc.boolD(this, "numbering", lineNumbering);
    defWrapPairs = gc.str(this, "wrapPairs", "codearea.wrapPairs");
    lnL = gc.em/2;
    lnR = gc.em;
  }
  
  public void setLang(Language l) {
    lang = l;
    updLang();
  }
  
  public void updLang() {
    langInst = lang.inst(f);
    modFrom(0, 0);
    for (int i = 0; i < lns.sz; i++) Arrays.fill(lns.get(i).st.arr, (byte) 0);
    for (Line c : lns) c.clearPara();
    mProp();
  }
  
  public void setFamily(String family) {
    super.setFamily(family);
    if (lang!=null) updLang();
  }
  
  public void drawC(Graphics g) {
    drawOffX = lineNumbering? lnL+lnR+f.width(lns.sz+"") : 0;
    super.drawC(g);
    if (lineNumbering) {
      int dy = drawOffY+f.ascI;
      g.rect(0, 0, drawOffX, h, lnBg);
      for (int i = 0; i < lns.sz; i++) {
        String s = (i+1) + "";
        int y = ln(i).yw*f.hi;
        if (g.clip!=null && y>g.clip.ey) break;
        g.text(s, f, drawOffX-lnR-f.width(s), y+dy, lnCol);
      }
    }
  }
  
  public Line createLine(char[] p, int y) { return new CodeLine(p, y); }
  public CodeLine ln(int y) {
    return (CodeLine) lns.get(y);
  }
  
  public void hidden() { super.hidden();
    for (int i = 0; i < lns.sz; i++) ln(i).clearPara(); // don't waste enormous amounts of RAM
  }
  public /*open*/ void insert(int x, int y, char p, boolean human) {
    super.insert(x, y, p, human);
    if (p=='\n' && human) {
      if (!multiline) return;
      CodeLine l = ln(y);
      int lw = l.leadingWs();
      calcEnd(y);
      if (l.end!=null) lw+= Math.max(0,l.end.depthDelta)*langInst.indentLen;
      char[] a = new char[lw]; Arrays.fill(a, langInst.indentChar);
      insert(0, y+1, a);
    }
  }
  
  public int minH(int w) {
    return super.minH(w) + padLns*f.hi;
  }
  
  public int minW() {
    return super.minW() + (int)(padChrs*f.monoWidth);
  }
  
  public void modFrom(int y, int dy) {
    if (dy!=0) ph = -1;
    for (int i = y; i < lns.sz; i++) {
      CodeLine ln = ln(i);
      if (ln.startDirty && dy==0) return;
      ln.startDirty = true;
      ln.y+= dy;
    }
  }
  
  private int calcNs;
  private static final int MAX_NS = 200000; // .2ms/frame max; excludes the resulting paragraph rebuilding, which will take much more
  public void tickC() { super.tickC();
    calcNs = 0;
  }
  public void calcEnd(int ey) {
    if (calcNs>MAX_NS) { mRedraw(); return; }
    int sy = ey;
    while (sy>=0 && ln(sy).startDirty) sy--;
    LangState<?> c = langInst.init; // should only be used if sy==-1
    for (int i = Math.max(0, sy); i <= ey; i++) {
      CodeLine l = ln(i);
      if (l.startDirty && !c.equals(l.start)) {
        l.start = c;
        l.end = null;
      }
      l.startDirty = false;
      if (l.end == null) {
        long sns = System.nanoTime();
        l.getEnd();
        calcNs+= System.nanoTime()-sns;
        if (calcNs>MAX_NS) { mRedraw(); return; }
      }
      c = l.end;
    }
  }
  
  
  public class CodeLine extends Line {
    public boolean startDirty = true;
    public LangState<?> start, end;
    public CodeLine(char[] p, int y) { super(p, y); }
    public void init(char[] p) {
      super.init(p);
    }
    
    protected void mod() { super.mod();
      modFrom(y+1, 0);
      end = null;
    }
    private void getEnd() {
      clearPara();
      end = start.after(sz(), a.arr, st.arr);
    }
    
    public void draw(Graphics g, int gy, CIt cit) {
      calcEnd(y);
      super.draw(g, gy, cit);
    }
  
    public TextStyle style(byte b) {
      return langInst.style(b);
    }
  }
  
  private void cursorPerLine() {
    Cursor[] todo = cs.get(0, cs.sz, Cursor[].class);
    for (Cursor c : todo) {
      c.order();
      for (int y=c.sy, ey=c.ey; y<= ey; y++) {
        if (y==c.sy)     c.mv(0, y, ln(y).sz(), y);
        else addCursor(cs.sz, 0, y, ln(y).sz(), y);
      }
    }
    sortCursors();
  }
  
  public boolean action2(String name) {
    switch (name) {
      case "cursorDown": {
        um.pushU("add cursor");
        Cursor n = addCursor(cs.sz, cs.peek());
        n.down(0);
        um.pop();
        return true;
      }
      case "cursorUp": {
        um.pushU("add cursor");
        Cursor n = addCursor(0, cs.get(0));
        n.up(0);
        sortCursors();
        um.pop();
        return true;
      }
      
      case "indentInc":
        um.pushQ("change indent");
        for (Cursor c : cs) for (int y = c.ym(); y <= c.yM(); y++) {
          insert(0, y, langInst.indent(1));
        }
        um.pop();
        return true;
      
      case "indentDec":
        um.pushQ("change indent");
        for (Cursor c : cs) for (int y = c.ym(); y <= c.yM(); y++) {
          if (ln(y).leadingWs()>=langInst.indentLen) remove(0, y, langInst.indentLen, y);
        }
        um.pop();
        return true;
      
      case "selectLines":
        um.pushU("make cursor per selected line");
        cursorPerLine();
        um.pop();
        return true;
      
      case "selectNext": {
        um.pushU("select more");
        boolean sel = anySel();
        if (!sel) {
          for (Cursor c : cs) {
            Line l = ln(c.sy);
            int sx = c.sx;
            int ex = c.ex;
            int sz = l.sz();
            while (sx>0 && isName(l.get(sx-1))) sx--;
            while (ex<sz && isName(l.get(ex))) ex++;
            c.mv(sx, c.sy, ex, c.sy);
          }
        } else {
          Cursor last = cs.get(cs.sz-1);
          last.order();
          String val = getByCursor(last);
    
          XY pos = searchString(val, last.ex, last.ey, ln(lns.sz-1).sz(), lns.sz-1);
          if (pos==null) {
            int px = 0, py = 0;
            for (int i = 0; i < cs.sz; i++) {
              Cursor c = cs.get(i);
              pos = searchString(val, px, py, c.sx, c.sy);
              if (pos!=null) break;
              px = c.ex; py = c.ey;
            }
          }
        
          if (pos!=null) {
            Cursor c = addCursor(cs.sz, last);
            c.mv(pos.x, pos.y, last.sy==last.ey? pos.x+last.ex-last.sx : last.ex, pos.y + last.ey-last.sy);
            sortCursors();
          }
        }
        um.pop();
        return true;
      }
      case "duplicateSelection": {
        um.pushQ("duplicate selection");
        for (Cursor c : cs) {
          String s = getByCursor(c);
          c.mv(c.xM(), c.yM());
          replaceByCursor(c, s);
        }
        um.pop();
        return true;
      }
      
      case "deleteLineBack": case "deleteLineNext": {
        um.pushQ("delete line");
        for (Cursor c : cs) {
          c.order();
          if (c.ey+1==lns.sz) {
            if (c.sy==0) {
              remove(0, c.sy, lns.peek().sz(), c.ey);
            } else {
              remove(lns.get(c.sy-1).sz(), c.sy-1, lns.peek().sz(), c.ey);
            }
          } else {
            remove(0, c.sy, 0, c.ey+1);
            int y = name.equals("deleteLineBack") && c.sy!=0? c.sy-1 : c.sy;
            c.mv(lns.get(y).sz(), y);
          }
        }
        um.pop();
        return true;
      }
      
      case "align": {
        um.pushL("align cursors");
        align: {
          int py = -1;
          for (Cursor c : cs) {
            if (py==c.sy) break align;
            py = c.sy;
            if (c.sy!=c.ey) {
              cursorPerLine();
              break;
            }
          }
          for (Cursor c : cs) c.order();
          int sx = cs.get(0).sx;
          int len = 0;
          for (Cursor c : cs) {
            sx = Math.max(sx, c.sx);
            len = Math.max(len, c.ex-c.sx);
          }
          for (Cursor c : cs) {
            insert(c.sx, c.sy, Tools.repeat(' ', sx - c.sx));
            insert(c.ex, c.ey, Tools.repeat(' ', len - (c.ex-c.sx)));
          }
        }
        um.pop();
        return true;
      }
      default: return false;
    }
  }
  
  
  public void typed(int codepoint) {
    // TODO be nice around typing closing half
    if (anySel()) {
      String wr = customWrapPairs!=null? customWrapPairs : defWrapPairs;
      for (int i=0, j=0; i < wr.length();) {
        int p = wr.codePointAt(i);
        i+= Character.charCount(p);
        if (j%2==0 && p==codepoint) {
          int match = wr.codePointAt(i);
          um.pushQ("wrap");
          for (Cursor c : cs) {
            StringBuilder b = new StringBuilder();
            b.appendCodePoint(codepoint);
            b.append(getByCursor(c));
            b.appendCodePoint(match);
            replaceByCursor(c, b.toString());
            c.sx+= Character.charCount(codepoint);
            c.ex-= Character.charCount(match);
          }
          um.pop();
          return;
        }
        j++;
      }
    }
    super.typed(codepoint);
  }
  
  
  public boolean keyF(Key key, int scancode, KeyAction a) {
    if (action2(gc.keymap(key, a, "codearea"))) return true;
    
    if (a.typed && key.k_home()) { // home to after leading ws
      boolean margin = true;
      for (Cursor c : cs) margin&= ln(c.ey).leadingWs()==c.ex;
      if (margin || key.hasCtrl()) return super.keyF(key, scancode, a);
      um.pushU("move cursor");
      for (Cursor c : cs) {
        c.mv(key.hasShift(), ln(c.ey).leadingWs(), c.ey);
        c.px = -1;
      }
      sortCursors();
      um.pop();
      return true;
    }
    return super.keyF(key, scancode, a);
  }
}
