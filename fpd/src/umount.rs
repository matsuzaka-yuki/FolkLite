use std::ffi::CString;
use std::fs;
use std::path::PathBuf;

const MNT_DETACH: libc::c_int = 2;

fn umount_lazy(path: &str) -> bool {
    let c_path = match CString::new(path) {
        Ok(s) => s,
        Err(_) => return false,
    };
    let ret = unsafe { libc::umount2(c_path.as_ptr(), MNT_DETACH) };
    if ret != 0 {
        let errno = unsafe { *libc::__errno() };
        eprintln!("unmount fail: {path} (errno={errno})");
        return false;
    }
    true
}

fn config_path() -> PathBuf {
    std::env::current_exe()
        .expect("failed to determine executable path")
        .parent()
        .expect("executable has no parent directory")
        .parent()
        .expect("executable parent has no grandparent directory")
        .join("UmountPATH")
}

pub fn run() -> u32 {
    let config = config_path();
    let content = match fs::read_to_string(&config) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("cannot read {}: {e}", config.display());
            return 1;
        }
    };

    let mut failures = 0u32;
    for line in content.lines() {
        let path = line.trim();
        if path.is_empty() {
            continue;
        }
        if umount_lazy(path) {
            eprintln!("unmount ok: {path}");
        } else {
            eprintln!("unmount fail: {path}");
            failures += 1;
        }
    }
    failures
}
