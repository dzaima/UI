package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public abstract class Lang {
  public final LangState<?> init;
  protected Lang(LangState<?> init) {
    this.init = init;
  }
  
  public static class LangInst {
    public final Lang l;
    public final Font font;
    private final TextStyle[] styles;
    
    public final int indentLen;
    public final char indentChar;
    
    protected LangInst(Lang l, Font font, TextStyle[] styles) {
      this.l = l;
      this.font = font;
      this.styles = styles;
      indentLen = 2;
      indentChar = ' ';
      indent = new char[indentLen]; Arrays.fill(indent, indentChar);
    }
    public TextStyle style(byte b) {
      return styles[b];
    }
    
    private final char[] indent;
    public char[] indent(int w) {
      if (w==1) return indent;
      char[] r = new char[w*indentLen];
      Arrays.fill(r, indentChar);
      return r;
    }
  }
  
  public LangInst forFont(Font f) {
    return new LangInst(this, f, genStyles(f));
  }
  
  protected abstract TextStyle[] genStyles(Font f);
  
  
  
  protected static TextStyle[] colors(int[] cols, Font f) {
    TextStyle[] r = new TextStyle[cols.length];
    for (int i = 0; i < cols.length; i++) r[i] = new TextStyle().setFontSize(f.sz).setFontFamily(f.tf.name).setColor(cols[i]);
    return r;
  }
}
