use serde::Serialize;
use std::net::IpAddr;

#[derive(Serialize)]
pub struct Verdict { pub is_safe: bool, pub risk_score: u8, pub reason: String }

/// Offline, bounded-cost URL triage. Heuristic signal only, not a malware verdict.
pub fn inspect_url(raw_url: &str) -> Verdict {
    let input = raw_url.trim();
    if input.is_empty() || input.len() > 2048 { return result(100, "URL boş veya izin verilen uzunluğu aşıyor"); }
    let normalized = if input.contains("://") { input.to_owned() } else { format!("https://{input}") };
    let lower = normalized.to_ascii_lowercase();
    let Some((scheme, rest)) = lower.split_once("://") else { return result(80, "URL biçimi tanınmadı"); };
    if scheme != "https" && scheme != "http" { return result(90, "Desteklenmeyen bağlantı protokolü"); }
    let mut score: u8 = 0;
    let mut reasons: Vec<&str> = Vec::new();
    if scheme == "http" { score = score.saturating_add(15); reasons.push("Şifrelenmemiş HTTP bağlantısı"); }
    let authority = rest.split(|c| c == '/' || c == '?' || c == '#').next().unwrap_or("");
    let host_port = authority.rsplit('@').next().unwrap_or("");
    if authority.contains('@') { score = score.saturating_add(25); reasons.push("Adres içinde kullanıcı bilgisi bulunuyor"); }
    let host = host_port.split(':').next().unwrap_or("").trim_start_matches('[').trim_end_matches(']');
    if host.parse::<IpAddr>().is_ok() { score = score.saturating_add(20); reasons.push("Alan adı yerine doğrudan IP adresi kullanılıyor"); }
    if host.starts_with("xn--") || host.contains(".xn--") { score = score.saturating_add(15); reasons.push("Uluslararası karakter kodlaması içeren alan adı"); }
    if host.split('.').count() > 5 { score = score.saturating_add(10); reasons.push("Alışılmadık derecede fazla alt alan adı"); }
    if lower.contains("%00") || lower.contains("%0d") || lower.contains("%0a") { score = score.saturating_add(20); reasons.push("Şüpheli kodlanmış kontrol karakteri"); }
    let terms = ["login-verify", "account-secure", "password-reset", "wallet-connect"];
    if terms.iter().any(|term| lower.contains(term)) { score = score.saturating_add(15); reasons.push("Kimlik doğrulama taklidiyle ilişkili URL kalıbı"); }
    score = score.min(100);
    let reason = if reasons.is_empty() { "Belirgin risk işareti bulunmadı; bu sonuç güvenlik garantisi değildir".to_owned() } else { reasons.join(" | ") };
    Verdict { is_safe: score < 40, risk_score: score, reason }
}

fn result(score: u8, reason: &str) -> Verdict { Verdict { is_safe: false, risk_score: score, reason: reason.to_owned() } }

#[cfg(test)] mod tests {
    use super::inspect_url;
    #[test] fn accepts_normal_https_url_as_low_risk() { let v = inspect_url("https://example.com"); assert!(v.is_safe); assert_eq!(v.risk_score, 0); }
    #[test] fn flags_plain_http_without_network_access() { let v = inspect_url("http://example.com"); assert!(v.risk_score >= 15); assert!(!v.reason.is_empty()); }
    #[test] fn rejects_unsupported_schemes() { assert!(!inspect_url("javascript:alert(1)").is_safe); }
    #[test] fn handles_empty_and_oversized_input() { assert!(!inspect_url("").is_safe); assert!(!inspect_url(&"a".repeat(2049)).is_safe); }
}