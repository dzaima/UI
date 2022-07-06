package dzaima.utils;

public class ColorUtils {
  
  
  public static int alpha(int argb) { return (argb>>24)&0xff; }
  public static int red  (int argb) { return (argb>>16)&0xff; }
  public static int green(int argb) { return (argb>> 8)&0xff; }
  public static int blue (int argb) { return  argb     &0xff; }
  public static byte alphaB(int argb) { return (byte)(argb>>24); }
  public static byte redB  (int argb) { return (byte)(argb>>16); }
  public static byte greenB(int argb) { return (byte)(argb>> 8); }
  public static byte blueB (int argb) { return (byte)(argb    ); }
  
  public static int argb(int a, int r, int g, int b) {
    return a<<24 | r<<16 | g<<8 | b;
  }
  
  public static float[] RGBtoHSB(int argb, float[] buf) {
    java.awt.Color.RGBtoHSB(red(argb), green(argb), blue(argb), buf);
    return buf;
  }
  public static int HSBtoRGB(int alpha, float hue, float sat, float br) {
    return java.awt.Color.HSBtoRGB(hue, sat, br)&0xffffff | alpha<<24;
  }
  
  public static float brightness(int argb) {
    return (float) ((0.2126f*red(argb) + 0.7152f*green(argb) + 0.0722f*blue(argb)) / 255.0);
  }
  
  public static int bw(int alpha, int value) {
    return argb(alpha, value, value, value);
  }
  public static int bw(int alpha, float brightness) {
    return bw(alpha, (int) (Tools.constrain(brightness, 0, 1)*255));
  }
  
  public static String format(int c) {
    String s = Integer.toUnsignedString(c, 16);
    if (s.length()<8) s = Tools.repeat('0', 8-s.length()) + s;
    else if (s.startsWith("ff")) s = s.substring(2);
    return "#"+s;
  }
  
  public static int lerp(int a, int b, float f) {
    float i = 1-f;
    return argb(
      (int)(alpha(a)*i + alpha(b)*f),
      (int)(red  (a)*i + red  (b)*f),
      (int)(green(a)*i + green(b)*f),
      (int)(blue (a)*i + blue (b)*f)
    );
  }
  
  public static Integer parsePrefixed(String s) {
    if (s.startsWith("#")) return parse(s.substring(1));
    else return parse(s);
  }
  
  public static Integer parse(String s) { // doesn't expect a starting '#'
    try {
      int p = Integer.parseUnsignedInt(s, 16);
      if (s.length()==8) return p;
      if (s.length()==7) return p|p>>>24<<28;
      if (s.length()==6) return p|0xff000000;
      if (s.length()==3) p|= 0xf000;
      if (s.length()==3 || s.length()==4) return ((p>>12&15)<<24 | (p>>8&15)<<16 | (p>>4&15)<<8 | (p&15)) * 0x11;
      if (s.length()==2) return p*0x010101 | 0xff000000;
      if (s.length()==1) return p*0x111111 | 0xff000000;
      return null;
    } catch (Throwable t) {
      return null;
    }
  }
}
