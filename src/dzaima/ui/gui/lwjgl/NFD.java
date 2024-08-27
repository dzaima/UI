package dzaima.ui.gui.lwjgl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import java.nio.file.*;
import java.util.function.Consumer;

public class NFD {
  public static void openFileStatic(String filter, Path initial, Consumer<Path> onResult) {
    PointerBuffer r = MemoryUtil.memAllocPointer(1);
    try {
      int x = NativeFileDialog.NFD_OpenDialog(filter==null? "" : filter, initial==null? null : initial.toAbsolutePath().toString(), r);
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    } finally {
      MemoryUtil.memFree(r);
    }
  }
  public static void openFolderStatic(Path initial, Consumer<Path> onResult) {
    PointerBuffer r = MemoryUtil.memAllocPointer(1);
    try {
      int x = NativeFileDialog.NFD_PickFolder(initial==null? null : initial.toAbsolutePath().toString(), r);
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    } finally {
      MemoryUtil.memFree(r);
    }
  }
  public static void saveFileStatic(String filter, Path initial, Consumer<Path> onResult) {
    PointerBuffer r = MemoryUtil.memAllocPointer(1);
    try {
      int x = NativeFileDialog.NFD_SaveDialog(filter==null? "" : filter, initial==null? null : initial.toAbsolutePath().toString(), r);
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    } finally {
      MemoryUtil.memFree(r);
    }
  }
}
