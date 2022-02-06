package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;
import io.github.humbleui.skija.paragraph.TextStyle;

public class TextLang extends Lang {
  public static int[] cols = { 0xffD2D2D2 };
  public TextStyle[] styles;
  public TextStyle style(byte v) {
    return styles[v];
  }
  
  public TextLang(Font f) {
    super(new TextState());
    styles = Lang.colors(cols, f);
  }
  public Lang font(Font f) { return new TextLang(f); }
  
  static class TextState extends LangState<TextState> {
    public TextState after(int sz, char[] p, byte[] b) { return this; }
    public boolean equals(Object obj) { return obj instanceof TextStyle; }
    public int hashCode() { return 0; }
  }
}