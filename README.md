# Cleanthes
> *"Guard it as Cleanthes guarded virtue."*

A zero-knowledge AES-256-GCM encrypted password vault for Android.
No cloud. No backdoors. No second chances.

Named after Cleanthes of Assos (331-230 BC), Zeno's Stoic successor.
He worked nights as a water-carrier to fund his philosophy by day.
The inner citadel is yours alone.

---

## What makes it secure

Your master password never leaves your phone. It is never sent to a server.
It cannot be recovered by anyone, including the developer.

When you create a vault, this happens:

    Your password + random salt
            ↓
    310,000 rounds of PBKDF2 hashing
    (meets OWASP 2023 recommendations)
            ↓
    An AES-256 encryption key (lives in RAM only, never saved to disk)

When an entry is saved:

    Your entry's password
            ↓
    AES-256-GCM encryption (unique random IV per entry)
            ↓
    Stored in SQLite database as ciphertext

Without the correct master password, the database is unreadable noise.

---

## Threat Model

**Cleanthes protects against:**
- Anyone with physical access to your device who does not know your master password
- Apps that read your storage
- Network-level attackers (no internet permission is declared)

**Cleanthes does not protect against:**
- A fully compromised OS or rooted device under attacker control
- Someone who already knows your master password
- Hardware-level forensics on an unlocked device

---

## Security guarantees

- Nothing sensitive is written to disk in plaintext
- No internet permission — the app makes zero network calls
- No cloud sync, no analytics, no telemetry
- Screenshots disabled on all screens (FLAG_SECURE)
- Session auto-locks after 5 minutes of inactivity
- 5 failed attempts triggers a 30-second lockout
- Android backup disabled — vault excluded from phone backups

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java (Android SDK 21–34) |
| Architecture | MVVM with ViewModel and LiveData |
| Database | SQLite with raw SQL |
| Encryption | AES-256-GCM via javax.crypto |
| Key Derivation | PBKDF2WithHmacSHA256 (310,000 iterations) |
| Secure Storage | EncryptedSharedPreferences + Android Keystore |
| Biometrics | BiometricPrompt API |
| UI | Material Components 3, ConstraintLayout |

---

## Features

- Zero-knowledge vault
- AES-256-GCM encryption with unique IV per entry
- Password strength meter (5-segment, real-time)
- FORGE — built-in password generator with configurable complexity
- Category organisation with chip filter
- Priority entry marking
- Biometric unlock (fingerprint)
- Session auto-lock with manual lock option
- Swipe to delete with undo
- Live search by title or username
- Copy password or username from list and detail screen
- Show/hide password toggle

---

## Project Structure

    cleanthes/
    |
    +-- data/
    |   +-- db/           SQLite schema and queries
    |   +-- entities/     VaultEntry model
    |   +-- repository/   Encrypts before write, decrypts after read
    |
    +-- security/
    |   +-- CryptoManager.java    AES-256-GCM encrypt and decrypt
    |   +-- KeyDerivation.java    PBKDF2 key derivation
    |   +-- BiometricHelper.java  Fingerprint unlock
    |
    +-- ui/
    |   +-- auth/      Setup, Login, SessionManager
    |   +-- home/      Vault list, search, filter
    |   +-- addedit/   Create and edit entries
    |   +-- detail/    Read-only entry view
    |
    +-- utils/
        +-- PasswordGenerator.java   FORGE password logic

---

## Build and Run

**Requirements:** JDK 17+, Android SDK 34, device or emulator on API 21+

**Android Studio (recommended):**
1. Clone the repo and open the project folder in Android Studio
2. Let Gradle sync
3. Run on a device or emulator

**Command line:**

    git clone https://github.com/YOUR_USERNAME/cleanthes.git
    cd cleanthes
    ./gradlew assembleDebug

Install the APK from `app/build/outputs/apk/debug/`

---

## The Name

Cleanthes of Assos (331–230 BC) succeeded Zeno as head of the Stoic school.
He is remembered not for volume but for discipline.

*"The willing are led by fate. The unwilling are dragged."*

---

## License

MIT — see LICENSE file

---

Built by FavourDevLabs — https://favourdevlabs.dev
