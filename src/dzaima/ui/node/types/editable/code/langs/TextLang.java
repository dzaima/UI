package dzaima.ui.node.types.editable.code.langs;

import dzaima.ui.gui.Font;
import dzaima.ui.node.types.editable.code.*;
import io.github.humbleui.skija.paragraph.TextStyle;

public class TextLang extends Lang {
  public static int[] cols = { 0xffD2D2D2 };
  protected TextStyle[] genStyles(Font f) {
    return colors(cols, f);
  }
  public TextLang() {
    super(new TextState());
  }
  
  static class TextState extends LangState<TextState> {
    public TextState after(int sz, char[] p, byte[] b) { return this; }
    public boolean equals(Object obj) { return obj instanceof TextStyle; }
    public int hashCode() { return 0; }
  }
}