#!/usr/bin/env python3
import sys, os, platform, urllib.request, subprocess, shutil, hashlib

def fail(msg):
  print(msg)
  sys.exit(1)

def shstr(s):
  return "'"+s.replace("'", "'\\''")+"'"

separate_output = False
output_dir = os.path.abspath('.')
java_cmd = 'java'
skip_ui = False
incremental = False
extra_jvm_flags = ''
extra_javac_args = []
cmd_prefix = ''
keep_lib = False
pre_17 = False
lib_os = None
lib_arch = None
update_download_hashes = False
override_main = None
components = {
  'jwm': True, # disabling JWM doesn't work, whatever
  'lwjgl': False,
}
for arg in sys.argv[1:]:
  if arg == 'skipui':
    skip_ui = True
  elif arg == 'i':
    incremental = True
  elif arg == 'keeplib':
    keep_lib = True
  elif arg == 'update-download-hashes':
    update_download_hashes = True
  elif arg.startswith('output='):
    output_dir = os.path.abspath(arg[7:])
    separate_output = True
    os.makedirs(output_dir, exist_ok=True)
  elif arg.startswith('java-cmd='):
    java_cmd = arg[9:]
  elif arg.startswith('jvm-args='):
    extra_jvm_flags += ' '+arg[9:]
  elif arg.startswith('javac-arg='):
    extra_javac_args += [arg[10:]]
  elif arg.startswith('jvm-arg='):
    extra_jvm_flags += ' '+shstr(arg[8:])
  elif arg.startswith('cmd-prefix='):
    cmd_prefix += arg[11:]+' '
  elif arg.startswith('override-main='):
    override_main = arg[14:]+' '
  elif arg.startswith('os='):
    lib_os = arg[3:]
    if not lib_os in ['linux', 'macos', 'windows']:
      fail(f'Unknown OS: {lib_os}')
      sys.exit(1)
  elif arg.startswith('arch='):
    lib_arch = arg[5:]
    if not lib_arch in ['x64', 'arm64']:
      fail(f'Unknown architecture: {lib_arch}')
  elif arg.startswith('incl='):
    components[arg[5:]] = True
  elif arg.startswith('excl='):
    components[arg[5:]] = False
  else:
    fail(f'Bad argument: "{arg}"')

if lib_os is None:
  if platform.system() == 'Linux':
    lib_os = 'linux'
  elif platform.system() == 'Darwin':
    lib_os = 'macos'
  elif 'Win' in platform.system():
    lib_os = 'windows'
  else:
    print(f'Unsupported OS "{platform.system()}"; assuming Linux')
    lib_os = 'linux'

if lib_arch is None:
  if platform.machine().lower() in ['x86_64', 'amd64']:
    lib_arch = 'x64'
  elif platform.machine().lower() in ['aarch64', 'arm64']:
    lib_arch = 'arm64'
  else:
    fail(f'Unsupported architecture "{platform.machine()}"')

# lib_os='linux';lib_arch='x86_64'
# lib_os='macos';lib_arch='x86_64'
# lib_os='macos';lib_arch='arm64'
# lib_os='windows';lib_arch='x86_64'

skija_os = lib_os + '-' + lib_arch
lwjgl_os = lib_os + ('' if lib_arch=='x64' else '-'+lib_arch)

lwjgl_version = '3.3.6'

pj = os.path.join
def at_out(where):
  return pj(output_dir, where)
def copy_new(src, dst):
  mkdirs(os.path.dirname(dst))
  shutil.copyfile(src, dst)
def mkdirs(path):
  os.makedirs(path, exist_ok=True)

def call(cmd):
  exit_code = subprocess.call(cmd)
  if exit_code:
    sys.exit(1)
  

def maven_lib(base, name, version, dir, expected, post = '', repo = 'https://repo1.maven.org/maven2'): # downloads into CWD/{dir}/..., and copies to output if that's different
  jar_name = name+'-'+version+post+'.jar'
  fname = dir+'/'+jar_name
  fname_tmp = fname+'.download'
  if not os.path.exists(fname):
    print('downloading '+jar_name)
    if os.path.dirname(fname):
      mkdirs(os.path.dirname(fname))
    
    url = repo+'/'+base+'/'+name+'/'+version+'/'+jar_name
    with open(fname_tmp, 'wb') as f:
      f.write(urllib.request.urlopen(url).read())
    f = open(fname_tmp, 'rb')
    sha256got = hashlib.sha256(f.read()).hexdigest()
    
    if isinstance(expected, str):
      sha256exp = [expected]
    else:
      sha256exp = [x for x in [x.split('-') for x in expected] if x[0]==lib_os and x[1]==lib_arch][0]
    
    if sha256got != sha256exp[-1]:
      print(f'unexpected sha256: got "{sha256got}", expected "{sha256exp[-1]}"')
      if update_download_hashes:
        print(f'updating build.py with new expected hash')
        with open('build.py', 'r') as f:
          pre = "'" + ''.join([x+'-' for x in sha256exp[:-1]])
          post = "'"
          text = f.read().replace(pre+sha256exp[-1]+post, pre+sha256got+post) # global replace, but that's actually, like, perfectly fine
          with open('build.py', 'w') as f:
            f.write(text)
      else:
        sys.exit(1)
    shutil.move(fname_tmp, fname)
  
  if at_out(fname) != os.path.abspath(fname):
    copy_new(fname, at_out(fname))
  return fname

# note that this function is copied in many places
def git_lib(path):
  if os.path.exists(path): return path
  path2 = path+'Clone'
  print(f'using {path2} submodule; link custom path to {path} to override')
  subprocess.check_call(['git', 'submodule', 'update', '--init', path2])
  return path2

def jar(res, classpath, release=''): # cwd should be a folder with src/, and will make classes/ there
  res = at_out(res)
  if not incremental and os.path.exists('classes/'):
    shutil.rmtree('classes/')
  mkdirs('classes/')
  srcs = []
  prev_classes = []

  def rec_file(parent, me, left):
    path = parent+me
    if not components['lwjgl'] and path in ['/dzaima/ui/gui/lwjgl/LwjglWindow.java', '/dzaima/ui/gui/lwjgl/LwjglManager.java']:
      return left
    
    src_path = 'src'+path
    if os.path.isdir(src_path):
      cls_path = 'classes'+path
      mkdirs(cls_path)
      prev_classes.append(cls_path)

      files = os.listdir(src_path)

      new_left = [x for x in os.listdir(cls_path) if x.endswith('.class')]
      for f in files:
        new_left = rec_file(path+'/', f, new_left)

      for c in new_left:
        # print('removing', f'classes{path}/{c}')
        os.remove(f'classes{path}/{c}') # remove files that aren't intended to be preserved

      return left
    else:
      if not me.endswith('.java'): fail(f'Unexpected extension for src{path}')
      cls = me[0:-5]
  
      def mine(s):
        return s.startswith(cls+'$') or s==cls+'.class'
      my_fs = [x for x in left if mine(x)]
      ot_fs = [x for x in left if not mine(x)]
  
      src_mod = os.path.getmtime(src_path)
      if len(my_fs)==0 or any([os.path.getmtime('classes'+parent+x)<src_mod for x in my_fs]):
        srcs.append(src_path)
        return left
      else:
        return ot_fs
  
  rec_file('', '', [])
  
  if len(srcs)>0:
    # print(srcs)
    call([
      'javac',
      *(['--release', release] if release else []),
      '-classpath', ':'.join([at_out(x) for x in classpath] + prev_classes),
      *extra_javac_args,
      '-Xmaxerrs', '1000',
      '-d', 'classes',
      *srcs
    ])
  call(['jar', 'cf', res, '-C', 'classes', '.'])

def build_ui_lib(uiloc):
  prev = os.getcwd()
  os.chdir(uiloc)
  ui_jar = 'lib/uilib/UI.jar'
  cp = build_ui(at_out(ui_jar))
  os.chdir(prev)
  
  copy_res(['base'], from_root = uiloc, force = True)
  
  return cp+[ui_jar]

def make_run(path, classpath, main, flags = ''):
  win = lib_os == 'windows'
  if win:
    path += '.bat'
  path = at_out(path)
  if override_main is not None:
    main = override_main
  flags+= extra_jvm_flags
  if not pre_17:
    flags+= ' --enable-native-access=ALL-UNNAMED'
  
  if win:
    run = f"""@echo off
setlocal
set "APPDIR=%~dp0"
if "%APPDIR:~-1%"=="\\" set "APPDIR=%APPDIR:~0,-1%"
set "RES_DIR=%APPDIR%\\res"
set "RES_DIR=%RES_DIR: =^ %"
{java_cmd} -DRES_DIR=%RES_DIR% {flags} -cp "{';'.join(['%APPDIR%\\'+x.replace("/", "\\") for x in classpath])}" {main} %*
endlocal
"""
  else:
    run = f"""#!/usr/bin/env bash
APPDIR=`readlink -f "$0"`
APPDIR=`dirname "$APPDIR"`
{cmd_prefix}{java_cmd} -DRES_DIR="$APPDIR/res/" {flags} -cp {':'.join(['"$APPDIR/"'+shstr(x) for x in classpath])} {main} "$@"
"""

  with open(path, 'w') as f:
    f.write(run)
  os.chmod(path, 0o777)

def copy_res(what = None, from_root = None, force = False): # copies <from_root || $CWD>/res/<what || (everything except base)/> to <output>/res
  if not separate_output and not force:
    return
  
  def from_file(path):
    if from_root is None:
      return path
    else:
      return pj(from_root, path)
  
  if what is None:
    what = os.listdir(from_file('res'))
    what = filter(lambda c: c!='base', what)
  
  for w in what:
    file = pj('res', w)
    src = from_file(file)
    dst = at_out(file)
    if os.path.isdir(src):
      shutil.copytree(src, dst, dirs_exist_ok=True)
    else:
      copy_new(src, dst)

def build_ui(full_res_path): # cwd must be of the UI repo
  libdir = 'lib/uilib'
  classpath = [
    maven_lib('io/github/humbleui', 'types', '0.2.0', libdir, '38d94d00770c4f261ffb50ee68d5da853c416c8fe7c57842f0e28049fc26cca8'),
    maven_lib('io/github/humbleui', 'skija-shared', '0.143.5', libdir, '44214d56c560ac543d175c24777d909c6366d3f548b611f0346f96e17753d447'),
    maven_lib('io/github/humbleui', 'skija-'+skija_os, '0.143.5', libdir, ['linux-x64-2abe075d40b6cf54879169f1cea7d4fa15fbd956ff24cea1f2e2b26b57a6f4b6','linux-arm64-975740d22a53be7d2dc607649bec01f7d36bba381ad82b1b7d695c52f54885eb','macos-arm64-4f3f6d59161b4fd6c1f76af6557acd6f433260989d4cd29132b3fdb7bd7e35fc','macos-x64-701d8a628d67f6b51a5e2f46d85023d4639594ce52fcaec40c2eecd61bdefd45','windows-x64-00243d40db34a4df114ef297b3f6d39f905d6feabb43f9340a672a953e3ef28e']),
  ]
  
  if components['jwm']: classpath+= [
    maven_lib('io/github/humbleui', 'jwm', '0.4.22', libdir, '7acc4904437ef16db7731f6f825134773f83b3f6bbadc995bc91ae16542abcf6'),
  ]
  
  lwjgl_native = '-natives-'+lwjgl_os
  def lwjgl_lib(name, post, sha256):
    return maven_lib('org/lwjgl', name, lwjgl_version, pj(libdir, 'lwjgl-'+lwjgl_version), sha256, post)
  
  # nfd needed by both JWM and LWJGL
  classpath+= [
    lwjgl_lib('lwjgl', '', 'b00e2781b74cc829db9d39fb68746b25bb7b94ce61d46293457dbccddabd999c'),
    lwjgl_lib('lwjgl', lwjgl_native, ['linux-x64-2f0e65d6985d602c0e5e5aab6576db113a9a378a36f8144ec93b77a4fde5876c','linux-arm64-07643ee5e95635b710715b41900c2a05c3f08c74be9309ba0763e31431bfad3b','macos-arm64-e68652629a28f0262ca99a2132d370e3fbc70d9f10e11717afa3f2dbe85b64fc','macos-x64-a818cba530f8a541ef30e2cc2b731c8de2d081d25e9901959ff2bcdbf2344e0f','windows-x64-a8d8edda34718bf70f68d14de1295b5bfc0f477a9607a3a9705d9e2d88538a8c']),
    lwjgl_lib('lwjgl-nfd', '', 'd43a10930f7980913d21e77d40318ecab84248dce63f5a4994a2a72a258c2af0'),
    lwjgl_lib('lwjgl-nfd', lwjgl_native, ['linux-x64-a17367975158de57aa711bfedc5383fbb089252104e67831548e242d336b320b','linux-arm64-5c0eca9bfd13465b00178fdebae3b4172fca36c76e1ba6d172ec982e43f8b616','macos-arm64-5ccc94ad816deb9e8e23e9e786d6a384a339b6be8191c39ecb9ff93e2d54e04c','macos-x64-a93f5646d191ecd217c4d498877959476723360eae6e00b4bbc9236aca6d5bfc','windows-x64-f3cb61462ca8972a74c98f462effcd4e8f5d3d87bbbcee8b13cd0ba7921e988d']),
  ]
  
  if components['lwjgl']: classpath+= [
    lwjgl_lib('lwjgl-glfw', '', 'b29c938ecc4997ce256a831aeca2033dab9829b5c21e72ebeb64aecd9e08450c'),
    lwjgl_lib('lwjgl-opengl', '', 'bb0430e0a5fd7b7b5770ae517960b2ea9887bb08d04f3a0cb1aae145332e1310'),
    lwjgl_lib('lwjgl-glfw',   lwjgl_native, ['linux-x64-a1b60014597bc0e45bf39089f4d838c3aa87fd668f6fe4e7326aa314d2ec87c0','linux-arm64-6a108ac764f88e0fc5b3c7de21227e7db123b1b27be531a93afb5954db4efa3f','macos-arm64-b46d40f15c2534e3800c5c44f73a4c7ded8a73b2272512c47847c1004ef7ffa9','macos-x64-826f9da50850d3e7e3b2002897b672cbd999d6d8a174ceea1d6e874d148c4bc1','windows-x64-7492d3f62a868f857173d85360bb58716cd3fe8563da18419dde858aed2deb41']),
    lwjgl_lib('lwjgl-opengl', lwjgl_native, ['linux-x64-8c0b5c081a7872a3cdb02b6448921da5ae5c23ab49656f299edc7a09b7e99b74','linux-arm64-5bb6b9052f40df5d62fb43f06561b82307e4d50c48ab596ef73b9b2a59c446c1','macos-arm64-2c0f67e7d36d71beed503043c06141af9fd83f5126a726eefceb7b5ba2aaf99c','macos-x64-660fdc9f4f06083938b9e60ab7a3ce9bc9eb6d1c7e60cb54228796101b18b633','windows-x64-b54a9d98686284947270e11e94c02aa15c30522119e7b80fcf0c98da6fa3c83c']),
  ]
  # lwjgl_lib('lwjgl', '-sources'),
  # lwjgl_lib('lwjgl-glfw', '-sources'),
  # lwjgl_lib('lwjgl-nfd', '-sources'),
  # lwjgl_lib('lwjgl-opengl', '-sources'),
  
  
  if not skip_ui:
    jar(full_res_path, classpath, '8' if pre_17 else None)
  return classpath


if __name__ == '__main__':
  cp = build_ui(at_out('UI.jar'))
  copy_res()
  copy_res(['base','../examples'])
  if separate_output:
    copy_new('src/dzaima/ui/node/types/editable/EditNode.java', at_out('res/EditNode.java'))
  make_run('run', cp+['UI.jar'], 'dzaima.ui.apps.ExMain', '-ea')