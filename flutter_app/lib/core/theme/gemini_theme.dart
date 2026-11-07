import 'package:flutter/material.dart';

class GeminiTheme {
  static const Color darkBackground = Color(0xFF131314);
  static const Color darkSurface = Color(0xFF1E1F20);
  static const Color darkSurfaceVariant = Color(0xFF282A2C);

  static const Color lightBackground = Color(0xFFF8FAFD);
  static const Color lightSurface = Color(0xFFFFFFFF);

  static const Color geminiBlue = Color(0xFF4B90FF);
  static const Color geminiPurple = Color(0xFF9B72CB);
  static const Color geminiPeach = Color(0xFFD96570);
  static const Color geminiCoral = Color(0xFFFF5546);

  static const LinearGradient auroraGradient = LinearGradient(
    colors: [geminiBlue, geminiPurple, geminiPeach, geminiCoral],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static ThemeData dark() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: darkBackground,
      colorScheme: const ColorScheme.dark(
        primary: geminiBlue,
        secondary: geminiPurple,
        tertiary: geminiCoral,
        surface: darkSurface,
      ),
      cardTheme: CardTheme(
        color: darkSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
    );
  }

  static ThemeData light() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      scaffoldBackgroundColor: lightBackground,
      colorScheme: const ColorScheme.light(
        primary: Color(0xFF1B6EF3),
        secondary: geminiPurple,
        tertiary: geminiCoral,
        surface: lightSurface,
      ),
      cardTheme: CardTheme(
        color: lightSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
    );
  }
}
