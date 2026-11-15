# Aegis — Hybrid Android Security Foundation

Aegis uses a lightweight Kotlin Android shell and a Rust native analysis core.

## Architecture
- Kotlin: Android lifecycle, foreground notification service, optional browser accessibility observer, permissions and JNI bridge.
- Rust: bounded-cost offline URL triage. The native core makes no outbound network requests.
- JNI: AegisNativeBridge loads the aegis_core shared library; CI builds ARM64, ARMv7 and x86_64 variants.
- Background work is event-driven. The service shell does not run a permanent polling loop.

## Device target
Product target: Android devices with at least 4 GB RAM and 32 GB storage. These are product requirements, not an install gate. Android's app-visible data partition is not a reliable way to infer advertised physical storage capacity. Validate performance on real low-end 4 GB devices.

## Current limitations
This branch is a technical foundation, not a finished security product. URL signals are heuristic and can produce false positives or miss threats; they are not antivirus-grade detection. The browser observer warns but does not automatically close pages. Root/Shizuku capability detection exists, but global proxy/firewall mutations are disabled until reversible transactions and device tests exist.

The foreground service uses Android's specialUse foreground-service category. Google Play distribution requires a valid declared use case and policy review. This does not bypass Android background-execution limits.

## Validation
CI runs Rust unit tests, cross-compiles the native library for supported ABIs and builds the Android release APK. Device-level battery, memory, thermal, permission-revocation, reboot and false-positive tests remain required before public release.
