package com.payscope.app.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// PayScope Premium Dark Palette Constants
val DarkDeepBackground = Color(0xFF040711)       // App canvas
val DarkSecondaryCanvas = Color(0xFF080D19)      // Secondary canvas
val DarkCardSurface = Color(0xFF0D1322)          // Card surface
val DarkElevatedCardSurface = Color(0xFF151D2E)  // Raised card surface
val DarkBorderSubtle = Color(0xFF232E40)         // Quiet surface separation
val DarkBorderHighlight = Color(0x33FF5500)      // Subtle orange glow border
val DarkBorderCyanHighlight = Color(0x3300E0E0)  // Subtle cyan glow border
val DarkTextPrimary = Color(0xFFF4F7FB)          // Pure crisp white
val DarkTextSecondary = Color(0xFFA4B0C2)        // Readable secondary copy
val DarkTextMuted = Color(0xFF8996AB)            // Readable supporting labels
val DarkSecondaryCyan = Color(0xFF00E0E0)
val DarkSecondaryCyanMuted = Color(0x2200E0E0)
val DarkSavingsTealMuted = Color(0x2214B8A6)
val DarkGlassBrushBg = Color(0xCC0D1322)

// PayScope Clean Light Palette Constants
val LightDeepBackground = Color(0xFFF5F7FA)       // App canvas
val LightSecondaryCanvas = Color(0xFFF1F5F9)      // Secondary canvas
val LightCardSurface = Color(0xFFFFFFFF)          // Card surface
val LightElevatedCardSurface = Color(0xFFF1F5F9)  // Raised card surface
val LightBorderSubtle = Color(0xFFE2E8F0)         // Subtle card border
val LightBorderHighlight = Color(0x22FF5500)      // Subtle orange border
val LightBorderCyanHighlight = Color(0x220284C7)  // Subtle cyan border
val LightTextPrimary = Color(0xFF0F172A)          // Dark slate
val LightTextSecondary = Color(0xFF475569)        // Medium slate
val LightTextMuted = Color(0xFF64748B)            // Muted hints
val LightSecondaryCyan = Color(0xFF0369A1)
val LightSecondaryCyanMuted = Color(0x180284C7)
val LightSavingsTealMuted = Color(0x180D9488)
val LightGlassBrush = Color(0xEEFFFFFF)

// Dynamic Theme Accessors (Composable)
val DeepBackground: Color @Composable get() = if (LocalIsDarkTheme.current) DarkDeepBackground else LightDeepBackground
val SecondaryCanvas: Color @Composable get() = if (LocalIsDarkTheme.current) DarkSecondaryCanvas else LightSecondaryCanvas
val CardSurface: Color @Composable get() = if (LocalIsDarkTheme.current) DarkCardSurface else LightCardSurface
val ElevatedCardSurface: Color @Composable get() = if (LocalIsDarkTheme.current) DarkElevatedCardSurface else LightElevatedCardSurface
val BorderSubtle: Color @Composable get() = if (LocalIsDarkTheme.current) DarkBorderSubtle else LightBorderSubtle
val BorderHighlight: Color @Composable get() = if (LocalIsDarkTheme.current) DarkBorderHighlight else LightBorderHighlight
val BorderCyanHighlight: Color @Composable get() = if (LocalIsDarkTheme.current) DarkBorderCyanHighlight else LightBorderCyanHighlight

// Brand Accents
val PrimaryOrange = Color(0xFFFF5500)        // Primary brand / action
val PrimaryOrangeHover = Color(0xFFFF6D22)
val SecondaryCyan: Color @Composable get() = if (LocalIsDarkTheme.current) DarkSecondaryCyan else LightSecondaryCyan
val SecondaryCyanMuted: Color @Composable get() = if (LocalIsDarkTheme.current) DarkSecondaryCyanMuted else LightSecondaryCyanMuted
val SavingsTeal: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2DD4BF) else Color(0xFF0F766E)
val SavingsTealMuted: Color @Composable get() = if (LocalIsDarkTheme.current) DarkSavingsTealMuted else LightSavingsTealMuted
val WarningAmber: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFBBF24) else Color(0xFF92400E)
val DangerRose: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFB7185) else Color(0xFFBE123C)

// Typography Colors
val TextPrimary: Color @Composable get() = if (LocalIsDarkTheme.current) DarkTextPrimary else LightTextPrimary
val TextSecondary: Color @Composable get() = if (LocalIsDarkTheme.current) DarkTextSecondary else LightTextSecondary
val TextMuted: Color @Composable get() = if (LocalIsDarkTheme.current) DarkTextMuted else LightTextMuted
val TextAccent: Color @Composable get() = if (LocalIsDarkTheme.current) DarkSecondaryCyan else LightSecondaryCyan

// Gradients & Accents
val DarkGlassBrush: Color @Composable get() = if (LocalIsDarkTheme.current) DarkGlassBrushBg else LightGlassBrush
val GlowOrange = Color(0x0AFF5500)
val GlowCyan = Color(0x0800E0E0)
val GlowTeal = Color(0x0A14B8A6)
