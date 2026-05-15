# proguard-rules.pro
#
# ProGuard shrinks, optimizes, and obfuscates the release APK.
# These rules tell it what NOT to touch — classes that are referenced
# by name at runtime (via reflection, XML inflation, etc.) would break
# if ProGuard renamed or removed them.
#
# ══════════════════════════════════════════════════════════
# HOW PROGUARD WORKS (brief):
# 1. SHRINK:    Remove unused classes, methods, fields
# 2. OPTIMIZE:  Inline methods, simplify logic
# 3. OBFUSCATE: Rename classes to a, b, c... (makes APK harder to reverse)
#
# The rules below are "-keep" rules — they preserve specific things
# from obfuscation or removal.
# ══════════════════════════════════════════════════════════

# ── VAULTX ENTITIES ─────────────────────────────────────────────────────────
# VaultEntry is constructed from a Cursor using setters called by name.
# If ProGuard renames getters/setters, the adapter breaks silently.
-keep class com.favourdevlabs.cleanthes.data.entities.** { *; }

# ── SECURITY CLASSES ────────────────────────────────────────────────────────
# javax.crypto classes are called reflectively by the JCA provider framework.
# Do not rename or remove any crypto-related classes.
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ── ENCRYPTED SHARED PREFERENCES ────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── BIOMETRIC ────────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── MATERIAL COMPONENTS ─────────────────────────────────────────────────────
# Chip, CardView, FAB, etc. are inflated from XML by class name
-keep class com.google.android.material.** { *; }

# ── LIFECYCLE / VIEWMODEL ────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }

# ── STANDARD ANDROID ─────────────────────────────────────────────────────────
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep all View subclasses (referenced from XML layouts by class name)
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ── SUPPRESS WARNINGS ────────────────────────────────────────────────────────
# EncryptedSharedPreferences uses Tink internally — suppress its notes
-dontnote com.google.crypto.tink.**
-dontwarn com.google.crypto.tink.**

