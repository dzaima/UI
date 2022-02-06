package dzaima.ui.gui.io;

import io.github.humbleui.jwm.Key;

import java.util.HashMap;

import static io.github.humbleui.jwm.Key.*;

public enum KeyVal {
  shift      (SHIFT,       "shift"),
  ctrl       (CONTROL,     "ctrl"),
  alt        (ALT,         "alt"),
  meta       (LINUX_META,  "meta"),
  mac_command(MAC_COMMAND, "command"),
  mac_option (MAC_OPTION,  "option"),
  mac_fn     (MAC_FN,      "function"),
  
  numLock    (NUM_LOCK,    "numlock"),
  capsLock   (CAPS_LOCK,   "capslock"),
  scrollLock (SCROLL_LOCK, "scrollock"),
  printScreen(PRINTSCREEN, "printscreen"),
  cancel     (CANCEL,      "cancel"),
  clear      (CLEAR,       "clear"),
  help       (HELP,        "help"),
  menu       (MENU,        "menu"),
  kana       (KANA,        "kana"),
  volumeUp   (VOLUME_UP,   "volup"),
  volumeDown (VOLUME_DOWN, "voldn"),
  mute       (MUTE,        "mute"),
  pause      (PAUSE,       "pause"),
  separator  (SEPARATOR,   "separator"),
  
  
  left (LEFT,     "left"),
  up   (UP,       "up"),
  right(RIGHT,    "right"),
  down (DOWN,     "down"),
  pgup (PAGE_UP,  "pgup"),
  pgdn (PAGE_DOWN,"pgdn"),
  home (HOME,     "home"),
  end  (END,      "end"),
  
  backspace(BACKSPACE,"backspace"),
  ins      (INSERT,   "insert"),
  del      (DELETE,   "delete"),
  esc      (ESCAPE,   "esc"),
  space    (SPACE,    "space"),
  tab      (TAB,      "tab"),
  enter    (ENTER,    "enter"),
  
  period      (PERIOD,        "period"),
  comma       (COMMA,         "comma"),
  semicolon   (SEMICOLON,     "semicolon"),
  quote       (QUOTE,         "quote"),
  backtick    (BACK_QUOTE,    "backtick"),
  add         (ADD,           "add"),
  minus       (MINUS,         "minus"),
  equal       (EQUALS,        "equal"),
  multiply    (MULTIPLY,      "multiply"),
  slash       (SLASH,         "slash"),
  backslash   (BACK_SLASH,    "backslash"),
  openBrak(OPEN_BRACKET,  "open bracket"),
  closeBrak(CLOSE_BRACKET, "close bracket"),
  
  a(A,"A"), f1 (F1, "F1" ), d0(DIGIT0,"0"),
  b(B,"B"), f2 (F2, "F2" ), d1(DIGIT1,"1"),
  c(C,"C"), f3 (F3, "F3" ), d2(DIGIT2,"2"),
  d(D,"D"), f4 (F4, "F4" ), d3(DIGIT3,"3"),
  e(E,"E"), f5 (F5, "F5" ), d4(DIGIT4,"4"),
  f(F,"F"), f6 (F6, "F6" ), d5(DIGIT5,"5"),
  g(G,"G"), f7 (F7, "F7" ), d6(DIGIT6,"6"),
  h(H,"H"), f8 (F8, "F8" ), d7(DIGIT7,"7"),
  i(I,"I"), f9 (F9, "F9" ), d8(DIGIT8,"8"),
  j(J,"J"), f10(F10,"F10"), d9(DIGIT9,"9"),
  k(K,"K"), f11(F11,"F11"),
  l(L,"L"), f12(F12,"F12"),
  m(M,"M"), f13(F13,"F13"),
  n(N,"N"), f14(F14,"F14"),
  o(O,"O"), f15(F15,"F15"),
  p(P,"P"), f16(F16,"F16"),
  q(Q,"Q"), f17(F17,"F17"),
  r(R,"R"), f18(F18,"F18"),
  s(S,"S"), f19(F19,"F19"),
  t(T,"T"), f20(F20,"F20"),
  u(U,"U"), f21(F21,"F21"),
  v(V,"V"), f22(F22,"F22"),
  w(W,"W"), f23(F23,"F23"),
  x(X,"X"), f24(F24,"F24"),
  y(Y,"Y"), f25(UNDEFINED,"F25"),
  z(Z,"Z"),
  unknown(UNDEFINED, "unknown key");
  public final Key jwmKey;
  public final String name;
  
  KeyVal(Key jwmKey, String name) {
    this.jwmKey = jwmKey;
    this.name = name;
  }
  
  
  public static final HashMap<Key, KeyVal> jwmMap = new HashMap<>();
  static {
    for (KeyVal k : KeyVal.values()) jwmMap.put(k.jwmKey, k);
    jwmMap.put(WIN_LOGO, KeyVal.meta);
    jwmMap.put(LINUX_SUPER, KeyVal.meta);
  }
  public static KeyVal of(Key jwmKey) {
    KeyVal r = jwmMap.get(jwmKey);
    assert r!=null && jwmKey!=null;
    return r;
  }
}
