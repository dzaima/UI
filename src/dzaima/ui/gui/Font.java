package dzaima.ui.gui;

import dzaima.utils.*;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.paragraph.*;

import java.util.*;

public class Font {
  public final Typeface tf;
  public final io.github.humbleui.skija.Font f;
  public final float asc, dsc, sz, h;
  public final int ascI, dscI, hi;
  public final Float strikeY, strikeH;
  public final Float underlineY, underlineH;
  public float monoWidth;
  
  public final int mode;
  
  Font(Typeface tf, float sz, int mode) {
    this.tf = tf;
    this.f = new io.github.humbleui.skija.Font(tf.tf, sz); // TODO italics & bold can go OOB so there needs to be some extra horizontal padding for nodes
    this.sz = sz;
    this.mode = mode;
    f.setAutoHintingForced(true);
    f.setHinting(FontHinting.SLIGHT);
    f.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
    f.setSubpixel(true);
    FontMetrics m = f.getMetrics();
    asc = -m.getAscent(); ascI = Tools.ceil(asc);
    dsc = m.getDescent(); dscI = Tools.ceil(dsc);
    strikeY = m.getStrikeoutPosition();
    strikeH = m.getStrikeoutThickness();
    underlineY = m.getUnderlinePosition();
    underlineH = m.getUnderlineThickness();
    h = ascI+dscI;
    hi = Tools.ceil(h);
    monoWidth = m.getAvgCharWidth();
  }
  
  
  public int width(String str) {
    return Tools.ceil(f.measureTextWidth(str)); // TODO think about trailing spaces
  }
  public float widthf(String str) {
    return f.measureTextWidth(str); // TODO think about trailing spaces
  }
  
  public Rect size(String s) {
    io.github.humbleui.types.Rect r = f.measureText(s);
    return new Rect((int)r.getLeft(), (int)r.getTop(), (int)r.getRight(), (int)r.getBottom()); // todo proper rounding?
  }
  
  public Font size(float nsz) {
    return tf.sizeMode(nsz, mode);
  }
  public Font sizeMode(float nsz, int mode) {
    return tf.sizeMode(nsz, mode);
  }
  public Font mode(int mode) {
    return tf.sizeMode(sz, mode);
  }
  
  
  HashMap<Integer, Boolean> hasChar = new HashMap<>();
  public boolean hasAll(String s) {
    IntArr arr = null;
    int i = 0;
    while (i < s.length()) {
      int p = s.codePointAt(i);
      if (p<' ' || p>'~') { // assume fonts have ASCII
        Boolean b = hasChar.get(p);
        if (b==Boolean.FALSE) return false;
        if (b==null) {
          if (arr==null) arr = new IntArr();
          arr.add(p);
        }
      }
      i+= Character.charCount(p);
    }
    if (arr!=null) {
      int[] got = arr.get();
      short[] res = f.getUTF32Glyphs(got);
      boolean selfOk = true;
      for (int j = 0; j < got.length; j++) {
        hasChar.put(got[j], res[j]!=0);
        if (res[j]==0) selfOk = false;
      }
      return selfOk;
    }
    return true;
  }
  static class IntArr {
    int[] arr = new int[8];
    int len;
    void add(int x) {
      if (len>=arr.length) arr = Arrays.copyOf(arr, arr.length*2);
      arr[len++] = x;
    }
    int[] get() {
      return Arrays.copyOf(arr, len);
    }
  }
  
  TextStyle ts;
  public TextStyle textStyle(int col) { // meant for temporary use, will get "corrupted" after another textStyle call
    if (ts==null) {
      ts = new TextStyle().setFontFamily(tf.name).setFontSize(sz).setFontStyle(tf.tf.getFontStyle());
      DecorationStyle ds = DecorationStyle.NONE;
      if ((mode&Typeface.UNDERLINE)!=0) ds = ds.withUnderline(true);
      if ((mode&Typeface.STRIKE   )!=0) ds = ds.withLineThrough(true);
      ds = ds.withThicknessMultiplier(2);
      ds = ds.withColor(col);
      ts.setDecorationStyle(ds);
    }
    ts.setColor(col);
    return ts;
  }
}
