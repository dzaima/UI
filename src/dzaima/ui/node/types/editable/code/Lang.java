package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public abstract class Lang {
  public final LangState<?> init;
  protected final int indentLen;
  protected final char indentChar;
  private final char[] indent;
  protected Lang(LangState<?> init) {
    this(init, 2, ' ');
  }
  protected Lang(LangState<?> init, int indentLen, char indentChar) {
    this.init = init;
    this.indentLen = indentLen;
    this.indentChar = indentChar;
    indent = new char[indentLen]; Arrays.fill(indent, indentChar);
  }
  
  public abstract TextStyle style(byte b);
  public abstract Lang font(Font f);
  
  protected static TextStyle[] colors(int[] cols, Font f) {
    TextStyle[] r = new TextStyle[cols.length];
    for (int i = 0; i < cols.length; i++) r[i] = new TextStyle().setFontSize(f.sz).setFontFamily(f.tf.name).setColor(cols[i]);
    return r;
  }
  
  public char[] indent(int w) {
    if (w==1) return indent;
    char[] r = new char[w*indentLen];
    Arrays.fill(r, indentChar);
    return r;
  }
}
