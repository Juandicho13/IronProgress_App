import SwiftUI

/// Tokens de Forja para la Live Activity (HANDOFF 12.1 y 12.10). La extensión no ve ForjaTheme:
/// si cambia un token en Kotlin, cambia aquí en el mismo commit.
enum ForjaTokens {
    /// Fondo negro siempre en la isla y la Live Activity.
    static let background = Color.black
    static let ember = Color(red: 1.0, green: 0x6B / 255.0, blue: 0x2C / 255.0)
    static let ice = Color(red: 0x6C / 255.0, green: 0xC4 / 255.0, blue: 0xF2 / 255.0)
    static let text1 = Color.white
    static let text2 = Color.white.opacity(0.64)
    static let surface3 = Color.white.opacity(0.14)
    /// Verde de Listo: el descanso terminó y toca la serie.
    static let ready = Color(red: 0x34 / 255.0, green: 0xC7 / 255.0, blue: 0x59 / 255.0)

    /// Cifras tabulares, como Archivo en la app (la extensión usa la del sistema).
    static func figure(_ size: CGFloat) -> Font {
        .system(size: size, weight: .semibold).monospacedDigit()
    }
}
