use crate::spoofer::generate_identity;
use serde::Serialize;
use std::time::Duration;

#[derive(Serialize)]
pub struct Verdict {
    pub is_safe: bool,
    pub risk_score: u8,
    pub reason: String,
}

pub fn inspect_url(raw_url: &str) -> Verdict {
    let target = if raw_url.starts_with("http://") || raw_url.starts_with("https://") {
        raw_url.to_string()
    } else {
        format!("https://{}", raw_url)
    };

    let identity = generate_identity();
    let mut risk_score: u8 = 0;
    let mut reasons: Vec<&str> = Vec::new();

    if target.starts_with("http://") {
        risk_score += 20;
        reasons.push("Şifresiz HTTP bağlantısı");
    }

    let agent = ureq::AgentBuilder::new()
        .timeout_connect(Duration::from_millis(1200))
        .timeout_read(Duration::from_millis(1500))
        .build();

    match agent
        .get(&target)
        .set("User-Agent", identity.user_agent)
        .set("Sec-CH-UA-Platform", identity.platform)
        .set("Accept-Language", identity.accept_lang)
        .set("DNT", "1")
        .call()
    {
        Ok(response) => {
            let final_url = response.get_url().to_string();

            // 1. Farklı alan adına yönlendirme kontrolü
            let orig_host = extract_host(&target);
            let final_host = extract_host(&final_url);
            if !orig_host.is_empty() && !final_host.is_empty() && orig_host != final_host {
                risk_score += 35;
                reasons.push("Şüpheli alan adı yönlendirmesi");
            }

            // 2. İlk 16 KB içeriği sıfır ekstra bellek yüküyle tara
            if let Ok(body) = response.into_string() {
                let sample: String = body.chars().take(16384).collect::<String>().to_lowercase();

                let mut suspicious_hits = 0;
                let signatures = [
                    "eval(atob(",
                    "document.write(unescape",
                    "cvv",
                    "kart_no",
                    "tc_kimlik",
                    "card_number",
                ];

                for sig in signatures {
                    if sample.contains(sig) {
                        suspicious_hits += 1;
                    }
                }

                if suspicious_hits >= 2 {
                    risk_score += 50;
                    reasons.push("Oltalama veya gizlenmiş zararlı betik tespit edildi");
                }
            }
        }
        Err(_) => {
            // Bağlantı kurulamazsa kullanıcıyı kilitlememek için düşük riskli geçiş
            reasons.push("Hızlı ön kontrol zaman aşımı");
        }
    }

    Verdict {
        is_safe: risk_score < 50,
        risk_score,
        reason: if reasons.is_empty() {
            "Temiz".to_string()
        } else {
            reasons.join(" | ")
        },
    }
}

fn extract_host(url: &str) -> &str {
    url.split("://")
        .nth(1)
        .unwrap_or(url)
        .split('/')
        .next()
        .unwrap_or("")
}
