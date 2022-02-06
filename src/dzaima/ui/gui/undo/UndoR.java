package dzaima.ui.gui.undo;

public abstract class UndoR<T> { // don't use for identity-ful `T`!
  public abstract T redoR();
  public abstract void undo();
}
