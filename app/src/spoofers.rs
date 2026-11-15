use rand::seq::SliceRandom;

pub struct SpoofedIdentity {
    pub user_agent: &'static str,
    pub platform: &'static str,
    pub accept_lang: &'static str,
}

const PROFILES: &[SpoofedIdentity] = &[
    SpoofedIdentity {
        user_agent: "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
        platform: "\"Android\"",
        accept_lang: "en-US,en;q=0.9",
    },
    SpoofedIdentity {
        user_agent: "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
        platform: "\"Android\"",
        accept_lang: "tr-TR,tr;q=0.9,en-US;q=0.8",
    },
    SpoofedIdentity {
        user_agent: "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
        platform: "\"iOS\"",
        accept_lang: "en-GB,en;q=0.9",
    },
];

pub fn generate_identity() -> &'static SpoofedIdentity {
    let mut rng = rand::thread_rng();
    PROFILES.choose(&mut rng).unwrap_or(&PROFILES[0])
}
