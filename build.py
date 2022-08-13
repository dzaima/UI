#!/usr/bin/env python3
import sys, os, urllib.request, subprocess, shutil

skija_os = "linux" # or "windows", or "macos-arm64", or "macos-x64"
lwjgl_os = "linux" # or "windows", or "macos-arm64", or "macos"
                   # (but there will probably be more changes needed for non-linux OSes probably)

lwjgl_version = "3.3.0"

def mkdirs(path):
  os.makedirs(path, exist_ok=True)

def call(cmd):
  exit_code = subprocess.call(cmd)
  if exit_code:
    sys.exit(1)
  

def maven_lib(base, name, version, dir, post = ""):
  jarname = name+"-"+version+post+".jar"
  fname = dir+"/"+jarname
  if not os.path.exists(fname):
    print("downloading "+jarname)
    if os.path.dirname(fname):
      mkdirs(os.path.dirname(fname))
    
    url = "https://repo1.maven.org/maven2/"+base+"/"+name+"/"+version+"/"+jarname
    with open(fname, 'wb') as f:
      f.write(urllib.request.urlopen(url).read())
  return fname

# note that this function is copied in many places
def git_lib(path, git):
  if os.path.exists(path): return path
  path2 = path+"Clone"
  print("using "+path2+" submodule; link custom path to "+path+" to override")
  subprocess.check_call(["git","submodule","update","--init",path2])
  return path2

def shstr(s):
  return "'"+s.replace("'", "'\\''")+"'"

def jar(res, classpath, release = ""):
  if not 'i' in sys.argv and os.path.exists("classes/"):
    shutil.rmtree("classes/")
  mkdirs("classes/")
  res = os.path.abspath(res)
  srcs = []
  prev_classes = []

  def rec_file(parent, me, left):
    path = parent+me
    src_path = "src"+path
    if os.path.isdir(src_path):
      cls_path = "classes"+path
      mkdirs(cls_path)
      prev_classes.append(cls_path)
      
      files = os.listdir(src_path)
      new_left = [x for x in os.listdir(cls_path) if x.endswith(".class")]
      for f in files:
        new_left = rec_file(path+"/", f, new_left)
      
      for c in new_left:
        # print("removing", "classes"+path+"/"+c)
        os.remove("classes"+path+"/"+c) # remove files that aren't intended to be preserved
      
      return left
    
    if not me.endswith(".java"): raise Exception("unexpected extension for src"+path)
    cls = me[0:-5]
    
    def mine(s):
      return s.startswith(cls+"$") or s==cls+".class"
    my_fs = [x for x in left if mine(x)]
    ot_fs = [x for x in left if not mine(x)]
    
    src_mod = os.path.getmtime(src_path)
    if len(my_fs)==0 or any([os.path.getmtime("classes"+parent+x)<src_mod for x in my_fs]):
      srcs.append(src_path)
      return left
    else:
      return ot_fs
  
  rec_file("", "", [])
  
  if len(srcs)>0:
    # print(srcs)
    call([
      "javac",
      *(["--release",release] if release else []),
      "-classpath", ':'.join(classpath+prev_classes),
      "-Xmaxerrs", "1000",
      "-d", "classes",
      * srcs
    ])
  call(["jar", "cf", res, "-C", "classes", "."])

def build_ui_lib(uiloc):
  mkdirs("lib/")
  uilib = "lib/uilib"
  prev = os.getcwd()
  os.chdir(uiloc)
  cp = build_ui(prev+"/lib/UI.jar")
  os.chdir(prev)
  res = "res/base"
  if not 'keeplib' in sys.argv:
    if os.path.exists(uilib): shutil.rmtree(uilib)
    shutil.copytree(uiloc+"/lib/", uilib)
  if os.path.exists(res): shutil.rmtree(res)
  shutil.copytree(uiloc+"/"+res, res)
  return ["lib/ui"+x for x in cp]+["lib/UI.jar"]

def make_run(path, classpath, main, flags = ""):
  run = f"""#!/usr/bin/env bash
APPDIR=`readlink -f "$0"`
APPDIR=`dirname "$APPDIR"`
java -DRES_DIR="$APPDIR/res/" {flags} -cp {':'.join(['"$APPDIR/"'+shstr(x) for x in classpath])} {main} "$@"
"""

  with open(path, 'w') as f:
    f.write(run)
  os.chmod("run", 0o777)


def build_ui(res = "UI.jar"):
  
  lwjgl_native = "-natives-"+lwjgl_os
  def lwjgl_lib(name, post = ""):
    return maven_lib("org/lwjgl", name, lwjgl_version, "lib/lwjgl-"+lwjgl_version, post)
  
  classpath = [
    maven_lib("io/github/humbleui", "types", "0.1.2", "lib"),
    maven_lib("io/github/humbleui", "jwm", "0.4.5", "lib"),
    maven_lib("io/github/humbleui", "skija-shared", "0.98.1", "lib"),
    maven_lib("io/github/humbleui", "skija-"+skija_os, "0.98.1", "lib"),
    
    lwjgl_lib("lwjgl"),
    lwjgl_lib("lwjgl-glfw"),
    lwjgl_lib("lwjgl-nfd"),
    lwjgl_lib("lwjgl-opengl"),
    # lwjgl_lib("lwjgl", "-sources"),
    # lwjgl_lib("lwjgl-glfw", "-sources"),
    # lwjgl_lib("lwjgl-nfd", "-sources"),
    # lwjgl_lib("lwjgl-opengl", "-sources"),
    lwjgl_lib("lwjgl", lwjgl_native),
    lwjgl_lib("lwjgl-glfw", lwjgl_native),
    lwjgl_lib("lwjgl-nfd", lwjgl_native),
    lwjgl_lib("lwjgl-opengl", lwjgl_native),
  ]
  
  if not 'skipui' in sys.argv:
    jar(res, classpath, "8")
  return classpath


if __name__ == "__main__":
  cp = build_ui()
  make_run("run", cp+["UI.jar"], "dzaima.ui.apps.ExMain", "-ea")