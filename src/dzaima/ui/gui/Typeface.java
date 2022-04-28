package dzaima.ui.gui;

import dzaima.utils.Log;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.paragraph.FontCollection;

import java.util.HashMap;

public class Typeface {
  public static final short ITALICS   = 1;
  public static final short BOLD      = 2;
  public static final short TF_MAX    = 4;
  
  public static final short STRIKE    = 4;
  public static final short UNDERLINE = 8;
  public static final short MODE_MAX  = 16;
  
  public static FontMgr fontMgr = FontMgr.getDefault();
  public static FontCollection fontCol = new FontCollection().setDefaultFontManager(fontMgr);
  
  public final io.github.humbleui.skija.Typeface tf;
  public final String name;
  public final int mode;
  public Typeface(io.github.humbleui.skija.Typeface tf, String name, int mode) {
    this.tf = tf;
    this.name = name;
    this.mode = mode;
  }
  
  public static final HashMap<String, Typeface> fonts = new HashMap<>();
  public static Typeface of(String name) {
    Typeface tf = fonts.get(name);
    if (tf==null) {
      fonts.put(name, tf = new Typeface(n(name, FontStyle.NORMAL), name, 0));
    }
    return tf;
  }
  private static io.github.humbleui.skija.Typeface n(String name, FontStyle s) {
    io.github.humbleui.skija.Typeface r = fontMgr.matchFamilyStyle(name, s);
    if (r==null) {
      Log.warn("Warning: Font \""+name+"\" not found; using fallback..");
      return fontCol.defaultFallback();
    }
    return r;
  }
  
  private Typeface[] tfs;
  public final HashMap<Integer, Font[]> sizes = new HashMap<>();
  public Font sizeMode(float sz, int nMode) {
    int tfm = nMode&(TF_MAX-1);
    if (tfs==null) {
      tfs = new Typeface[TF_MAX];
      tfs[mode] = this;
    }
    if (tfs[tfm]==null) {
      boolean it = (tfm&ITALICS)!=0;
      boolean b = (tfm&BOLD)!=0;
      tfs[tfm] = new Typeface(n(name, it? b?FontStyle.BOLD_ITALIC:FontStyle.ITALIC : b?FontStyle.BOLD:FontStyle.NORMAL), name, tfm);
      tfs[tfm].tfs = tfs;
    }
    float v = 1;
    int isz = Math.round(sz*v);
    Font[] f = sizes.computeIfAbsent(isz, k -> new Font[MODE_MAX]);
    if (f[nMode]==null) f[nMode] = new Font(tfs[tfm], isz/v, nMode);
    return f[nMode];
  }
}
