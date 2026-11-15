mod inspector;

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_nevruz_aegis_core_bridge_AegisNativeBridge_nativeInspectUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let raw: String = match env.get_string(&url) {
        Ok(value) => value.into(),
        Err(_) => {
            return env
                .new_string(r#"{"is_safe":false,"risk_score":0,"reason":"URL okunamadı"}"#)
                .expect("JNI string allocation failed")
                .into_raw();
        }
    };

    let verdict = inspector::inspect_url(&raw);
    let json = serde_json::to_string(&verdict).unwrap_or_else(|_| {
        r#"{"is_safe":false,"risk_score":0,"reason":"Analiz sonucu oluşturulamadı"}"#.to_owned()
    });

    env.new_string(json)
        .expect("JNI string allocation failed")
        .into_raw()
}
