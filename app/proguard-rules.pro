# Mantener información de genéricos y anotaciones
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, *Annotation*

# Reglas para GSON
-keepclassmembers class com.example.todoapp.** {
  public <init>();
}

-keep class com.example.todoapp.** { *; }
-keep class * implements java.io.Serializable { *; }

# Evitar ofuscar modelos y DTOs
-keep class com.example.todoapp.**.TaskEntity { *; }
-keep class com.example.todoapp.**.CategoryEntity { *; }
-keep class com.example.todoapp.holidays.data.model.** { *; }

# Reglas para Hilt
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclassmembers @dagger.hilt.InstallIn class * {
    <init>();
}

# Reglas para Retrofit y OkHttp
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Reglas para Room
-keep class * extends androidx.room.RoomDatabase {
    public static <methods>;
}

# Reglas para Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

