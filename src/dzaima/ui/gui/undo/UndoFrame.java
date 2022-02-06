package dzaima.ui.gui.undo;

import dzaima.utils.Vec;

public class UndoFrame {
  public final Vec<UndoR<?>> is = new Vec<>();
  public final String id;
  public final int mode;
  public final long time;
  public boolean important;
  
  public UndoFrame(String id, int mode, long time) {
    this.id = id;
    this.mode = mode;
    this.time = time;
    assert id!=null;
  }
  
  public void undo() {
    for (int i = is.sz-1; i >= 0; i--) is.get(i).undo();
  }
  public void redo() {
    for (UndoR<?> c : is) c.redoR();
  }
}
