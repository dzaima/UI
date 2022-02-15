package dzaima.ui.gui.select;

public interface Selectable {
  boolean selectS(Selection s);
  void selectE(Selection s);
  String selType();
  
  default boolean selectable() {
    return true;
  }
}
