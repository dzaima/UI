package dzaima.ui.gui.io;

import java.util.*;

public class Key {
  public final KeyVal val;
  public final int mod;
  
  public Key(KeyVal val, int mod) {
    assert val!=null;
    this.val = val;
    this.mod = mod;
  }
  
  @SuppressWarnings("PointlessBitwiseExpression")
  public static final int M_SHIFT = 1<<0;
  public static final int M_CTRL  = 1<<1;
  public static final int M_ALT   = 1<<2;
  public static final int M_SUP   = 1<<3;
  public static final int P_RIGHT = 1<<4;
  public static final int P_KP    = 1<<5;
  public static final int M_MOD = M_CTRL|M_SHIFT|M_ALT|M_SUP;
  
  public boolean onKeypad () { return (mod&P_KP)!=0; }
  public boolean onRight  () { return (mod&P_RIGHT)!=0; }
  public boolean isModifier() { return val.isModifier(); }
  
  public boolean plain() { return (mod&M_MOD) == 0; }
  public boolean only(int mask) { return (mod&M_MOD) == mask; }
  public boolean onlyCtrl () { return (mod&M_MOD) == M_CTRL ; } public boolean hasCtrl () { return (mod&M_CTRL )!=0; }
  public boolean onlyShift() { return (mod&M_MOD) == M_SHIFT; } public boolean hasShift() { return (mod&M_SHIFT)!=0; }
  public boolean onlyAlt  () { return (mod&M_MOD) == M_ALT  ; } public boolean hasAlt  () { return (mod&M_ALT  )!=0; }
  public boolean onlySuper() { return (mod&M_MOD) == M_SUP  ; } public boolean hasSuper() { return (mod&M_SUP  )!=0; }
  
  public static boolean ctrl (int mod) { return (mod&M_CTRL )!=0; }
  public static boolean shift(int mod) { return (mod&M_SHIFT)!=0; }
  public static boolean alt  (int mod) { return (mod&M_ALT  )!=0; }
  
  public static boolean only(int mod, int want) {
    return want == ( want & mod)
        &&    0 == (~want & mod & M_MOD);
  }
  
  public static boolean none(int mod) {
    return (mod&M_MOD) == 0;
  }
  
  
  public boolean k_shift      () { return val==KeyVal.shift;       }
  public boolean k_control    () { return val==KeyVal.ctrl;        }
  public boolean k_alt        () { return val==KeyVal.alt;         }
  public boolean k_meta       () { return val==KeyVal.meta;        }
  public boolean k_mac_command() { return val==KeyVal.mac_command; }
  public boolean k_mac_option () { return val==KeyVal.mac_option;  }
  public boolean k_mac_fn     () { return val==KeyVal.mac_fn;      }
  
  public boolean k_numLock    () { return val==KeyVal.numLock;     }
  public boolean k_capsLock   () { return val==KeyVal.capsLock;    }
  public boolean k_scrollLock () { return val==KeyVal.scrollLock;  }
  public boolean k_printScreen() { return val==KeyVal.printScreen; }
  public boolean k_cancel     () { return val==KeyVal.cancel;      }
  public boolean k_clear      () { return val==KeyVal.clear;       }
  public boolean k_help       () { return val==KeyVal.help;        }
  public boolean k_menu       () { return val==KeyVal.menu;        }
  public boolean k_kana       () { return val==KeyVal.kana;        }
  public boolean k_volumeUp   () { return val==KeyVal.volumeUp;    }
  public boolean k_volumeDown () { return val==KeyVal.volumeDown;  }
  public boolean k_mute       () { return val==KeyVal.mute;        }
  public boolean k_pause      () { return val==KeyVal.pause;       }
  public boolean k_separator  () { return val==KeyVal.separator;   }
  
  
  public boolean k_left () { return val==KeyVal.left;  }
  public boolean k_up   () { return val==KeyVal.up;    }
  public boolean k_right() { return val==KeyVal.right; }
  public boolean k_down () { return val==KeyVal.down;  }
  public boolean k_pgup () { return val==KeyVal.pgup;  }
  public boolean k_pgdn () { return val==KeyVal.pgdn;  }
  public boolean k_home () { return val==KeyVal.home;  }
  public boolean k_end  () { return val==KeyVal.end;   }
  
  public boolean k_backspace() { return val==KeyVal.backspace; }
  public boolean k_ins      () { return val==KeyVal.ins;       }
  public boolean k_del      () { return val==KeyVal.del;       }
  public boolean k_esc      () { return val==KeyVal.esc;       }
  public boolean k_space    () { return val==KeyVal.space;     }
  public boolean k_tab      () { return val==KeyVal.tab;       }
  public boolean k_enter    () { return val==KeyVal.enter;     }
  
  public boolean k_period      () { return val==KeyVal.period;       }
  public boolean k_comma       () { return val==KeyVal.comma;        }
  public boolean k_semicolon   () { return val==KeyVal.semicolon;    }
  public boolean k_quote       () { return val==KeyVal.quote;        }
  public boolean k_backtick    () { return val==KeyVal.backtick;     }
  public boolean k_add         () { return val==KeyVal.add;          }
  public boolean k_minus       () { return val==KeyVal.minus;        }
  public boolean k_equal       () { return val==KeyVal.equal;        }
  public boolean k_multiply    () { return val==KeyVal.multiply;     }
  public boolean k_slash       () { return val==KeyVal.slash;        }
  public boolean k_backslash   () { return val==KeyVal.backslash;    }
  public boolean k_openBracket () { return val==KeyVal.openBrak;  }
  public boolean k_closeBracket() { return val==KeyVal.closeBrak; }
  
  public boolean k_a() { return val==KeyVal.a; } public boolean k_f1 () { return val==KeyVal.f1;  } public boolean k_0() { return val==KeyVal.d0; }
  public boolean k_b() { return val==KeyVal.b; } public boolean k_f2 () { return val==KeyVal.f2;  } public boolean k_1() { return val==KeyVal.d1; }
  public boolean k_c() { return val==KeyVal.c; } public boolean k_f3 () { return val==KeyVal.f3;  } public boolean k_2() { return val==KeyVal.d2; }
  public boolean k_d() { return val==KeyVal.d; } public boolean k_f4 () { return val==KeyVal.f4;  } public boolean k_3() { return val==KeyVal.d3; }
  public boolean k_e() { return val==KeyVal.e; } public boolean k_f5 () { return val==KeyVal.f5;  } public boolean k_4() { return val==KeyVal.d4; }
  public boolean k_f() { return val==KeyVal.f; } public boolean k_f6 () { return val==KeyVal.f6;  } public boolean k_5() { return val==KeyVal.d5; }
  public boolean k_g() { return val==KeyVal.g; } public boolean k_f7 () { return val==KeyVal.f7;  } public boolean k_6() { return val==KeyVal.d6; }
  public boolean k_h() { return val==KeyVal.h; } public boolean k_f8 () { return val==KeyVal.f8;  } public boolean k_7() { return val==KeyVal.d7; }
  public boolean k_i() { return val==KeyVal.i; } public boolean k_f9 () { return val==KeyVal.f9;  } public boolean k_8() { return val==KeyVal.d8; }
  public boolean k_j() { return val==KeyVal.j; } public boolean k_f10() { return val==KeyVal.f10; } public boolean k_9() { return val==KeyVal.d9; }
  public boolean k_k() { return val==KeyVal.k; } public boolean k_f11() { return val==KeyVal.f11; }
  public boolean k_l() { return val==KeyVal.l; } public boolean k_f12() { return val==KeyVal.f12; }
  public boolean k_m() { return val==KeyVal.m; } public boolean k_f13() { return val==KeyVal.f13; }
  public boolean k_n() { return val==KeyVal.n; } public boolean k_f14() { return val==KeyVal.f14; }
  public boolean k_o() { return val==KeyVal.o; } public boolean k_f15() { return val==KeyVal.f15; }
  public boolean k_p() { return val==KeyVal.p; } public boolean k_f16() { return val==KeyVal.f16; }
  public boolean k_q() { return val==KeyVal.q; } public boolean k_f17() { return val==KeyVal.f17; }
  public boolean k_r() { return val==KeyVal.r; } public boolean k_f18() { return val==KeyVal.f18; }
  public boolean k_s() { return val==KeyVal.s; } public boolean k_f19() { return val==KeyVal.f19; }
  public boolean k_t() { return val==KeyVal.t; } public boolean k_f20() { return val==KeyVal.f20; }
  public boolean k_u() { return val==KeyVal.u; } public boolean k_f21() { return val==KeyVal.f21; }
  public boolean k_v() { return val==KeyVal.v; } public boolean k_f22() { return val==KeyVal.f22; }
  public boolean k_w() { return val==KeyVal.w; } public boolean k_f23() { return val==KeyVal.f23; }
  public boolean k_x() { return val==KeyVal.x; } public boolean k_f24() { return val==KeyVal.f24; }
  public boolean k_y() { return val==KeyVal.y; } public boolean k_f25() { return val==KeyVal.f25; }
  public boolean k_z() { return val==KeyVal.z; }
  
  public int number() {
    char c = val.name.charAt(0);
    if (c>='0' & c<='9') return c-'0';
    return -1;
  }
  
  public String repr() {
    StringBuilder r = new StringBuilder();
    if (hasCtrl()) r.append("ctrl+");
    if (hasAlt()) r.append("alt+");
    if (hasShift()) r.append("shift+");
    r.append(val.name.toLowerCase());
    return r.toString();
  }
  
  
  public static Key byName(String s) {
    String[] parts = s.split("\\+");
    int mod = 0;
    for (int i = 0; i < parts.length-1; i++) {
      switch (parts[i]) {
        case "shift": mod|= M_SHIFT; break;
        case "alt": mod|= M_ALT; break;
        case "ctrl": mod|= M_CTRL; break;
        case "windows": case "meta": case "super": mod|= M_SUP; break;
        case "kp": case "keypad": mod|= P_KP; break; 
        case "right": mod|= P_RIGHT; break;
        default: return null;
      }
    }
    Key k = keyBase.get(parts[parts.length - 1]);
    if (k==null) return null;
    return new Key(k.val, mod|k.mod);
  }
  
  
  private static final HashMap<String, Key> keyBase = new HashMap<>();
  static {
    for (KeyVal c : KeyVal.values()) {
      keyBase.put(c.name.toLowerCase(Locale.ROOT), new Key(c, 0));
    }
    keyBase.put(" ", new Key(KeyVal.space, 0));
    
    keyBase.put("del", keyBase.get("delete"));
    keyBase.put("ins", keyBase.get("insert"));
    keyBase.put("escape", keyBase.get("esc"));
    keyBase.put("bs", keyBase.get("backspace"));
    keyBase.put("pageup", keyBase.get("pgup"));
    keyBase.put("pagedown", keyBase.get("pgdn"));
    
    keyBase.put("!", new Key(KeyVal.d1, M_SHIFT));
    keyBase.put("@", new Key(KeyVal.d2, M_SHIFT));
    keyBase.put("#", new Key(KeyVal.d3, M_SHIFT));
    keyBase.put("$", new Key(KeyVal.d4, M_SHIFT));
    keyBase.put("%", new Key(KeyVal.d5, M_SHIFT));
    keyBase.put("^", new Key(KeyVal.d6, M_SHIFT));
    keyBase.put("&", new Key(KeyVal.d7, M_SHIFT));
    keyBase.put("*", new Key(KeyVal.d8, M_SHIFT));
    keyBase.put("(", new Key(KeyVal.d9, M_SHIFT));
    keyBase.put(")", new Key(KeyVal.d0, M_SHIFT));
    
    keyBase.put("`", new Key(KeyVal.backtick, 0)); keyBase.put("~", new Key(KeyVal.backtick, M_SHIFT));
    keyBase.put("-", new Key(KeyVal.minus,    0)); keyBase.put("_", new Key(KeyVal.minus,    M_SHIFT));
    keyBase.put("=", new Key(KeyVal.equal,    0)); keyBase.put("+", new Key(KeyVal.equal,    M_SHIFT));
    keyBase.put("\\",new Key(KeyVal.backslash,0)); keyBase.put("|", new Key(KeyVal.backslash,M_SHIFT));
    keyBase.put("[", new Key(KeyVal.openBrak, 0)); keyBase.put("{", new Key(KeyVal.openBrak, M_SHIFT));
    keyBase.put("]", new Key(KeyVal.closeBrak,0)); keyBase.put("}", new Key(KeyVal.closeBrak,M_SHIFT));
    keyBase.put(";", new Key(KeyVal.semicolon,0)); keyBase.put(":", new Key(KeyVal.semicolon,M_SHIFT));
    keyBase.put("'", new Key(KeyVal.quote,    0)); keyBase.put("\"",new Key(KeyVal.quote,    M_SHIFT));
    keyBase.put(",", new Key(KeyVal.comma,    0)); keyBase.put("<", new Key(KeyVal.comma,    M_SHIFT));
    keyBase.put(".", new Key(KeyVal.period,   0)); keyBase.put(">", new Key(KeyVal.period,   M_SHIFT));
    keyBase.put("/", new Key(KeyVal.slash,    0)); keyBase.put("?", new Key(KeyVal.slash,    M_SHIFT));
    
    for (char c='A'; c<='Z'; c++) keyBase.put(""+c, new Key(keyBase.get(""+Character.toLowerCase(c)).val, M_SHIFT));
  }
}
