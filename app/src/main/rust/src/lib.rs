mod inspector;
mod spoofer;

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_nevruz_aegis_core_bridge_AegisNativeBridge_nativeInspectUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("{\"is_safe\":true,\"risk_score\":0,\"reason\":\"\"}").unwrap().into_raw(),
    };

    let verdict = inspector::inspect_url(&url_str);
    let json = serde_json::to_string(&verdict).unwrap_or_else(|_| {
        "{\"is_safe\":true,\"risk_score\":0,\"reason\":\"\"}".to_string()
    });

    env.new_string(json).unwrap().into_raw()
}
