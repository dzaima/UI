package dzaima.ui.gui.jwm;

import io.github.humbleui.jwm.LayerGL;
import io.github.humbleui.skija.*;

public class SkijaLayerGL extends LayerGL {
  private final JWMWindow w;
  public DirectContext directContext;
  public BackendRenderTarget renderTarget;
  public Surface surface;
  
  public SkijaLayerGL(JWMWindow w) {
    this.w = w;
  }
  
  public void beforePaint() {
    makeCurrent();
    initParts();
  }
  
  public void initParts() {
    if (directContext == null) directContext = DirectContext.makeGL();
    if (renderTarget == null) renderTarget = BackendRenderTarget.makeGL(getWidth(), getHeight(), /*samples*/0, /*stencil*/8, /*fbId*/0, FramebufferFormat.GR_GL_RGBA8);
    if (surface == null) {
      surface = Surface.makeFromBackendRenderTarget(directContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB(), new SurfaceProps(PixelGeometry.RGB_H));
      w.newCanvas(surface);
    }
  }
  
  public void afterPaint() {
    surface.flushAndSubmit();
    swapBuffers();
  }
  
  public void skipPaint() {
    makeCurrent();
    initParts();
    surface.flushAndSubmit();
    swapBuffers();
  }
  
  public void resize(int width, int height) {
    super.resize(width, height);
    
    if (surface != null) {
      surface.close();
      surface = null;
    }
    
    if (renderTarget != null) {
      renderTarget.close();
      renderTarget = null;
    }
    
    if (directContext != null) {
      // directContext.abandon();
      directContext.close();
      directContext = null;
    }
  }
  
  public void close() {
    if (directContext != null) {
      // directContext.abandon();
      directContext.close();
      directContext = null;
    }
    
    if (surface != null) {
      surface.close();
      surface = null;
    }
    
    if (renderTarget != null) {
      renderTarget.close();
      renderTarget = null;
    }
    
    super.close();
  }
}
