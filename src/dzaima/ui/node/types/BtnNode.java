package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

import java.util.function.Consumer;

public class BtnNode extends Node {
  // for all styles
  private int style, padX, padY,
              bgOff, bgHover, bgOn;
  // style==1:
  private int bRad, bcolL, bcolD;
  // style==2:
  private int radius;
  private int widthMode; // 0-min; 1-max; 2-inherit
  public BtnNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  private boolean cursor;
  public void propsUpd() { super.propsUpd();
    String styleS = gc.val(this, "style", "btn.style");
    
    int padId = id("pad");
    if (padId==-1) {
      padX = gc.len(this, "padX", "btn.padX");
      padY = gc.len(this, "padY", "btn.padY");
    } else padX=padY = vs[padId].len();
    int wId = id("w");
    if (wId == -1) widthMode = 0;
    else switch (vs[wId].val()) { default: Log.error("node 'btn'", "'w' parameter must be either \"min\", \"max\", or \"inherit\""); break;
      case "min": widthMode=0; break;
      case "max": widthMode=1; break;
      case "inherit": widthMode=2; break;
    }
    
    cursor = gc.boolD(this, "cursor", true);
    
    switch (styleS) { default: Log.error("node 'bn'", "invalid style: '"+styleS+"'"); break;
      case "rect":
        style = 1;
        radius = 0;
        bRad = gc.len(this, "borderRadius", "btn.rect.borderRadius");
        bgOn = gc.col(this, "bg", "btn.rect.bg");
        bcolL = gc.col(this, "borderL", "btn.rect.borderL");
        bcolD = gc.col(this, "borderD", "btn.rect.borderD");
        break;
      case "round":
        style = 2;
        bRad = 0;
        radius = gc.len(this, "radius", "btn.round.radius");
        bgOff   = gc.col(this, "bgOff", "btn.round.bgOff");
        bgOn    = gc.col(this, "bgOn", "btn.round.bgOn");
        bgHover = gc.col(this, "bgHover", "btn.round.bgHover");
        break;
      case "box":
        style = 2;
        bRad = 0;
        radius = 0;
        bgOff   = gc.col(this, "bgOff", "btn.round.bgOff");
        bgOn    = gc.col(this, "bgOn", "btn.round.bgOn");
        bgHover = gc.col(this, "bgHover", "btn.round.bgHover");
        break;
    }
  }
  
  public /*open*/ void clicked() { if (fn!=null) fn.accept(this); }
  protected Consumer<BtnNode> fn;
  public void setFn(Consumer<BtnNode> fn) { this.fn = fn; }
  
  public void bg(Graphics g, boolean full) {
    switch (style) { default: throw new IllegalStateException();
      case 1:
        if (Tools.st(bgOn)) pbg(g, full);
        int b = bRad;
        int W = w-b; int H = h-b;
        g.poly(new float[]{0,0, w,0, W,b, b,b, b,H, 0,h}, clicked? bcolD : bcolL);
        g.poly(new float[]{w,h, w,0, W,b, W,H, b,H, 0,h}, clicked? bcolL : bcolD);
        g.rect(b, b, W, H, bgOn);
        break;
      case 2:
        pbg(g, full);
        g.rrect(0, 0, w, h, radius, clicked? bgOn : hovered()? bgHover : bgOff);
        break;
    }
  }
  
  public Node ch() { return ch.get(0); }
  public int getW(boolean max) { return ((widthMode==2? max : widthMode==1)? ch().maxW() : ch().minW())+(bRad+padX)*2; }
  public int minW() { return getW(false); }
  public int maxW() { return getW(true); }
  public int minH(int w) { return (ch().minH(w - (bRad+padX)*2)) + (bRad+padY)*2; }
  public int maxH(int w) { return minH(w); }
  
  public void resized() {
    ch().resize(w - (bRad+padX)*2, h - (bRad+padY)*2, bRad+padX, bRad+padY);
  }
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (c.bL()) c.register(this, x, y);
  }
  
  boolean clicked;
  public void mouseDown(int x, int y, Click c) {
    clicked = true;
    mRedraw();
  }
  
  public void mouseTick(int x, int y, Click c) {
    clicked = x>=0 & y>=0 && x<w & y<h;
    mRedraw();
    if (!clicked) c.unregister();
  }
  public void mouseUp(int x, int y, Click c) {
    if (clicked) clicked();
    clicked = false;
    mRedraw();
  }
  
  public boolean hovered() { return hoverIdx!=-1; }
  public short hoverIdx = -1;
  public void hoverS() { mRedraw(); hoverIdx = ctx.vw().pushCursor(null); }
  public void hoverE() { mRedraw(); hoverIdx = -1; ctx.vw().popCursor(); }
  public void hoverT(int mx, int my) { ctx.vw().replaceCursor(hoverIdx, cursor? Window.CursorType.HAND : null); }
}
