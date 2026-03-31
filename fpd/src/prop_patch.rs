use std::process::Stdio;

const RESETPROP: &str = "/data/adb/ap/bin/resetprop";

struct PropItem {
    key: &'static str,
    value: &'static str,
}

const PATCH_LIST: &[PropItem] = &[
    PropItem {
        key: "ro.boot.vbmeta.device_state",
        value: "locked",
    },
    PropItem {
        key: "ro.boot.verifiedbootstate",
        value: "green",
    },
    PropItem {
        key: "ro.boot.flash.locked",
        value: "1",
    },
    PropItem {
        key: "ro.boot.veritymode",
        value: "enforcing",
    },
    PropItem {
        key: "vendor.boot.vbmeta.device_state",
        value: "locked",
    },
    PropItem {
        key: "vendor.boot.verifiedbootstate",
        value: "green",
    },
    PropItem {
        key: "ro.boot.vbmeta.invalidate_on_error",
        value: "yes",
    },
    PropItem {
        key: "ro.boot.vbmeta.avb_version",
        value: "1.0",
    },
    PropItem {
        key: "ro.boot.vbmeta.hash_alg",
        value: "sha256",
    },
    PropItem {
        key: "ro.boot.vbmeta.size",
        value: "4096",
    },
    PropItem {
        key: "ro.boot.warranty_bit",
        value: "0",
    },
    PropItem {
        key: "ro.warranty_bit",
        value: "0",
    },
    PropItem {
        key: "ro.vendor.boot.warranty_bit",
        value: "0",
    },
    PropItem {
        key: "ro.vendor.warranty_bit",
        value: "0",
    },
    PropItem {
        key: "sys.oem_unlock_allowed",
        value: "0",
    },
    PropItem {
        key: "ro.build.type",
        value: "user",
    },
    PropItem {
        key: "ro.build.tags",
        value: "release-keys",
    },
    PropItem {
        key: "ro.secureboot.lockstate",
        value: "locked",
    },
    PropItem {
        key: "ro.debuggable",
        value: "0",
    },
    PropItem {
        key: "ro.force.debuggable",
        value: "0",
    },
    PropItem {
        key: "ro.secure",
        value: "1",
    },
    PropItem {
        key: "ro.adb.secure",
        value: "1",
    },
    PropItem {
        key: "ro.boot.realmebootstate",
        value: "green",
    },
    PropItem {
        key: "ro.boot.realme.lockstate",
        value: "1",
    },
    PropItem {
        key: "persist.logd.size",
        value: "",
    },
    PropItem {
        key: "persist.logd.size.crash",
        value: "",
    },
    PropItem {
        key: "persist.logd.size.system",
        value: "",
    },
    PropItem {
        key: "persist.logd.size.main",
        value: "",
    },
];

const BOOT_KEYS: &[&str] = &["ro.bootmode", "ro.boot.bootmode", "vendor.boot.bootmode"];

fn set_prop(key: &str, value: &str) {
    let _ = std::process::Command::new(RESETPROP)
        .args(["-n", key, value])
        .stderr(Stdio::null())
        .status();
}

fn get_prop(key: &str) -> Option<String> {
    let output = std::process::Command::new(RESETPROP)
        .arg(key)
        .stderr(Stdio::null())
        .output()
        .ok()?;

    let lossy = String::from_utf8_lossy(&output.stdout);
    let trimmed = lossy.trim();
    if trimmed.is_empty() {
        None
    } else {
        Some(trimmed.to_owned())
    }
}

fn patch_boot_keys() {
    for &key in BOOT_KEYS {
        if let Some(val) = get_prop(key) {
            if val.contains("recovery") {
                set_prop(key, "unknown");
            }
        }
    }
}

pub fn run() {
    for item in PATCH_LIST {
        if get_prop(item.key).is_some() {
            set_prop(item.key, item.value);
        }
    }
    patch_boot_keys();
}
