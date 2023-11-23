package dzaima.ui.gui;

import dzaima.ui.eval.*;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Native;
import io.github.humbleui.skija.paragraph.*;

public abstract class Graphics {
  public final boolean redraw = false; // might be un-final-ed at some point
  public int w, h;
  
  private final Vec<Rect> clips = new Vec<>();
  private final IntVec xoffs = new IntVec(); public int xoff;
  private final IntVec yoffs = new IntVec(); public int yoff;
  public Rect clip;
  public Canvas canvas;
  
  
  
  protected void init(Surface s) {
    canvas = s.getCanvas();
    w = s.getWidth();
    h = s.getHeight();
  }
  public abstract void close();
  
  
  
  private final Paint tempPaint = new Paint();
  private final long tempPaintPtr = Native.getPtr(tempPaint);
  public long paintP(int col) {
    Paint._nSetColor(tempPaintPtr, col);
    return tempPaintPtr;
  }
  public Paint paintO(int col) {
    Paint._nSetColor(tempPaintPtr, col);
    return tempPaint;
  }
  
  public void push() {
    clips.add(clip);
    xoffs.add(xoff);
    yoffs.add(yoff);
    canvas.save();
  }
  public void pop() {
    clip = clips.pop();
    xoff = xoffs.pop();
    yoff = yoffs.pop();
    canvas.restore();
  }
  
  public void clip(int x, int y, int w, int h) {
    clip(Rect.xywh(x, y, w, h));
  }
  public void clipE(int sx, int sy, int ex, int ey) {
    clip(new Rect(sx, sy, ex, ey));
  }
  public void clip(Rect r) {
    canvas.clipRect(r.skiaf());
    clip = clip==null? r : clip.and(r);
  }
  public void diffClipLocal(Rect r) { // doesn't count towards clip
    canvas.clipRect(r.skiaf(), ClipMode.DIFFERENCE, false);
  }
  
  public void rect(Rect r, int   fill) { rect(r.sx, r.sy, r.ex, r.ey, fill); }
  public void rect(Rect r, Paint fill) { rect(r.sx, r.sy, r.ex, r.ey, fill); }
  public void rect(float sx, float sy, float ex, float ey, Paint  p) { Canvas._nDrawRect(canvas._ptr, sx, sy, ex, ey, Native.getPtr(p)); }
  public void rect(float sx, float sy, float ex, float ey, int fill) { Canvas._nDrawRect(canvas._ptr, sx, sy, ex, ey, paintP(fill)); }
  public void rectWH(int x, int y, int w, int h, int fill) {
    rect(x, y, x+w, y+h, fill);
  }
  public void rrect(float sx, float sy, float ex, float ey, float r, int fill) {
    Canvas._nDrawRRect(canvas._ptr, sx, sy, ex, ey, new float[]{r}, paintP(fill));
  }
  public void rrect(float sx, float sy, float ex, float ey, float tl, float tr, float bl, float br, int fill) {
    Canvas._nDrawRRect(canvas._ptr, sx, sy, ex, ey, new float[]{tl, tr, bl, br}, paintP(fill));
  }
  
  public void poly(float[] coords, int fill) {
    Path p = new Path(); p.addPoly(coords, true);
    canvas.drawPath(p, paintO(fill));
    p.close();
  }
  
  public void tri(int x0, int y0, int x1, int y1, int x2, int y2, int fill) {
    poly(new float[]{x0, y0, x1, y1, x2, y2}, fill);
  }
  
  public void circle(float x, float y, float r, Paint color) {
    canvas.drawCircle(x, y, r, color);
  }
  
  public void translate(int dx, int dy) {
    canvas.translate(dx, dy);
    if (clip!=null) clip = clip.minus(dx, dy);
    xoff+= dx;
    yoff+= dy;
  }
  
  // these break clip bounds
  public void translateLocal(float dx, float dy) { canvas.translate(dx, dy); }
  public void scaleLocal(float x, float y) { canvas.scale(x, y); }
  
  public void clear(int col) {
    canvas.clear(col);
  }
  
  public void line(int x0, int y0, int x1, int y1, int col) {
    canvas.drawLine(x0+.5f, y0+.5f, x1+.5f, y1+.5f, paintO(col));
  }
  public void dashH(int x, int y, int l, Paint p) {
    canvas.drawLine(x, y+.5f, x+l, y+.5f, p);
  }
  public void dashV(int x, int y, int l, Paint p) {
    canvas.drawLine(x+.5f, y, x+.5f, y+l, p);
  }
  
  @SuppressWarnings("ConstantConditions") // __Y and __H have equal nullability
  public void text(String s, Font f, float x, float y, int col) {
    Paint p = paintO(col);
    if ((f.mode&Typeface.UNDERLINE)!=0 && f.underlineY!=null) {
      int sy = Math.round(y+f.underlineY);
      rect(x, sy-Math.max(1,f.underlineH), x+f.width(s), sy, p);
    }
    canvas.drawString(s, x, y, f.f, tempPaint);
    if ((f.mode&Typeface.STRIKE)!=0 && f.strikeY!=null) {
      int sy = Math.round(y+f.strikeY);
      rect(x, sy-Math.max(1,f.strikeH), x+f.width(s), sy, p);
    }
  }
  public static ParagraphStyle tmpParaStyle = new ParagraphStyle();
  public void textP(String s, Font f, float x, float y, int col) {
    if (f.hasAll(s)) text(s, f, x, y, col);
    else textPimpl(s, f, x, y, col);
  }
  public void textPimpl(String s, Font f, float x, float y, int col) {
    tmpParaStyle.setTextStyle(f.textStyle(col));
    ParagraphBuilder b = new ParagraphBuilder(tmpParaStyle, Typeface.fontCol);
    b.addText(s);
    Paragraph r = b.build();
    r.layout(Float.POSITIVE_INFINITY);
    b.close();
    r.paint(canvas, x, y-f.asc);
    r.close();
  }
  
  public static TextStyle textStyle(Prop family, int col, float sz) {
    if (family.type()=='{') {
      Vec<PNode> ch = family.gr().ch;
      String[] families = new String[ch.sz];
      for (int i = 0; i < ch.sz; i++) families[i] = ((PNode.PNodeStr) ch.get(i)).s;
      tmpTextStyle.setFontFamilies(families);
    } else {
      tmpTextStyle.setFontFamily(family.str());
    }
    tmpTextStyle.setFontSize(sz).setColor(col);
    return tmpTextStyle;
  }
  
  public static TextStyle tmpTextStyle = new TextStyle();
  public static Paragraph paragraph(TextStyle style, String text) {
    ParagraphBuilder pb = new ParagraphBuilder(tmpParaStyle.setTextStyle(style), Typeface.fontCol);
    pb.addText(text);
    Paragraph r = pb.build();
    r.layout(Float.POSITIVE_INFINITY);
    pb.close();
    return r;
  }
  
  public void image(Image i, int x, int y) {
    canvas.drawImage(i, x, y);
  }
  
  public enum Sampling {
    NEAREST(new FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE)),
    LINEAR(new FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)),
    LINEAR_MIPMAP(new FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)),
    MITCHELL(SamplingMode.MITCHELL),
    CATMULL_ROM(SamplingMode.CATMULL_ROM);
    
    private final SamplingMode m;
    Sampling(SamplingMode m) { this.m = m; }
  }
  public void image(Image i, int x, int y, int w, int h, Sampling s) {
    canvas.drawImageRect(i, io.github.humbleui.types.Rect.makeWH(i.getWidth(), i.getHeight()), io.github.humbleui.types.Rect.makeXYWH(x, y, w, h), s.m, null, true);
  }
  
  public static void pad(ParagraphBuilder b, int dx, int h) {
    if (dx>1) {
      try (TextStyle s = new TextStyle().setWordSpacing(dx-2).setFontSize(1).setColor(0)) {
        TextStyle._nSetHeight(s._ptr, true, h);
        b.pushStyle(s);
        b.addText("a b");
        b.popStyle();
      }
    }
  }
}
