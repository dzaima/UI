package dzaima.ui.eval;

import dzaima.ui.eval.PrsField.*;
import dzaima.ui.eval.Token.*;
import dzaima.utils.Vec;

public class Prs {
  private final Token[] arr;
  int off;
  
  public Prs(Vec<Token> arr) {
    this.arr = arr.toArray(new Token[0]);
  }
  
  public static PNodeGroup parseList(String s) {
    Prs p = new Prs(Tokenizer.tk(s));
    PNodeGroup r = p.nodeNamed(null, false);
    if (p.off != p.arr.length-1) throw p.err("Parse error: Expected end");
    return r;
  }
  public static PNodeGroup parseNode(String s) {
    Prs p = new Prs(Tokenizer.tk(s));
    PNodeGroup r = p.node();
    if (p.off != p.arr.length-1) throw p.err("Parse error: Expected end");
    return r;
  }
  
  
  private PNodeGroup node() {
    String name;
    NameTok t = (NameTok) req('v');
    name = t.s;
    req('{');
    PNodeGroup r = nodeNamed(name, t.defn);
    req('}');
    return r;
  }
  private PNodeGroup nodeNamed(String name, boolean defn) {
    Vec<PrsField> props = new Vec<>();
    Vec<PNode> ch = new Vec<>();
    while (true) {
      if (off >= arr.length-1) break;
      Token t = pop();
      if (t.type=='s') { // node: STR
        ch.add(new PNode.PNodeStr(((StrTok) t).s));
      } else if (t.type=='v') { // node: NAME …
        Token n = peek();
        if (n.type=='{') { // node: NAME '{' node* '}'
          off--;
          ch.add(node());
        } else if (n.type=='=') { // node: path '=' val
          off++;
          Token v = pop();
          String pname = ((NameTok) t).s;
          PrsField val;
          if (v.type=='v') {
            off--;
            if (peek1().type=='{') {
              val = new GroupFld(pname, node());
            } else if (((NameTok) v).defn) {
              val = new VarFld(pname, path());
            } else {
              val = new NameFld(pname, path());
            }
          } else if (v.type=='n') {
            NumTok v0 = (NumTok)v;
            if (get(':')) {
              Token v1 = pop();
              if (v1.type!='n') throw err("Parse error: Range ends must be numbers or units");
              val = new RangeFld(pname, v0, (NumTok) v1);
            } else {
              val = new NumFld(pname, v0.num, v0.s);
            }
          } else if (v.type=='s') {
            val = new StrFld(pname, ((StrTok) v).s);
          } else if (v.type=='#') {
            val = new ColFld(pname, ((ColorTok) v).c);
          } else if (v.type=='{') {
            val = new GroupFld(pname, nodeNamed(null, false));
            req('}');
          } else throw err("Parse error: Expected value, got "+v.expl());
          props.add(val);
        } else if (((NameTok) t).defn) {
          ch.add(new PNode.PNodeDefn(((NameTok) t).s));
        } else throw err("Parse error: Expected '='");
      } else if (t.type == '}') {
        off--;
        break;
      } else throw err("Parse error: Unexpected "+t.expl());
    }
    return new PNodeGroup(name, defn, props, ch);
  }
  private String path() {
    return ((NameTok) pop()).s;
  }
  
  private Token peek() {
    return arr[off];
  }
  private Token peek1() {
    return arr[off+1];
  }
  private Token pop() {
    return arr[off++];
  }
  private String fmtType(char t) {
    return t=='v'? "name" : t=='n'? "number" : t=='t'? "typed number" : t=='#'? "color" : t=='\0'? "EOF" : t=='s'? "string" : "'"+t+"'";
  }
  private Token req(char t) {
    Token f = pop();
    if (f.type != t) throw err("Parse error: Expected "+fmtType(t)+", got "+fmtType(f.type));
    return f;
  }
  private boolean get(char t) {
    if (peek().type==t) {
      pop();
      return true;
    }
    return false;
  }
  
  private RuntimeException err(String msg) {
    return new ParserException(msg+" at "+(off>=arr.length? arr[arr.length-1] : arr[off]).off);
  }
  public static class ParserException extends RuntimeException {
    public ParserException(String msg) { super(msg); }
  }
}
