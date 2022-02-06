package dzaima.ui.gui.io;

public enum KeyAction {
  PRESS  (true, false,false),
  REPEAT (false,true, false),
  RELEASE(false,false,true );
  
  public final boolean press;
  public final boolean typed;
  public final boolean repeat;
  public final boolean release;
  
  KeyAction(boolean press, boolean repeat, boolean release) {
    this.press = press;
    this.typed = press | repeat;
    this.repeat = repeat;
    this.release = release;
  }
}
