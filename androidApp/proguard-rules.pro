# ── Preflight release (R8) keep rules ───────────────────────────────────────
# Most libraries below ship their own consumer rules; these are belt-and-suspenders
# for the reflection-driven paths that R8 can't see statically.

# ── kotlinx.serialization ────────────────────────────────────────────────────
# R8 strips the synthetic $serializer companions otherwise → runtime
# SerializationException on history JSON and Compottie's Lottie parsing.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep generated serializers and the serializer() accessors for our @Serializable types.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# Our serializable models specifically.
-keep,includedescriptorclasses class com.example.appreviewer_1.**$$serializer { *; }
-keepclassmembers class com.example.appreviewer_1.** {
    *** Companion;
}

# ── Ktor / OkHttp ────────────────────────────────────────────────────────────
# Ktor ships consumer rules; silence the optional-dependency warnings R8 surfaces.
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.**
-dontwarn io.ktor.**

# ── ML Kit GenAI (Gemini Nano) ───────────────────────────────────────────────
# Google libs ship consumer rules; keep the public API surface defensively.
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**

# ── Compottie (Lottie) ───────────────────────────────────────────────────────
-keep class io.github.alexzhirkevich.compottie.** { *; }
-dontwarn io.github.alexzhirkevich.compottie.**
