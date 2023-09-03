package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;
import io.github.humbleui.skija.Image;

public class ImgNode extends Node { // TODO remove
  private String url;
  private float sc;
  public ImgNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    Log.warn("'img' node is deprecated");
  }
  
  
  public Image img;
  public int iw, ih;
  public int aw, ah;
  public void propsUpd() { super.propsUpd();
    updSize();
  }
  
  public void setImg(byte[] data) {
    try {
      img = Image.makeDeferredFromEncodedBytes(data); // TODO async
      iw = img.getWidth();
      ih = img.getHeight();
    } catch (Throwable e) {
      img = null;
      iw = ih = 10;
    }
    updSize();
  }
  private void updSize() {
    sc = gc.imgScale;
    int mwi = id("maxW"); if (mwi!=-1 && iw*sc > vs[mwi].len()) sc = vs[mwi].len()/(float)iw;
    int mhi = id("maxH"); if (mhi!=-1 && ih*sc > vs[mhi].len()) sc = vs[mhi].len()/(float)ih;
    aw = Tools.ceil(iw*sc);
    ah = Tools.ceil(ih*sc);
  }
  
  public Graphics.Sampling samplingMode = Graphics.Sampling.LINEAR_MIPMAP; // TODO option
  public void drawC(Graphics g) {
    if (img!=null) {
      g.image(img, 0, 0, aw, ah, samplingMode);
    }
  }
  
  public int minW(     ) { return aw; }
  public int maxW(     ) { return aw; }
  public int minH(int w) { return ah; }
  public int maxH(int w) { return ah; }
}
