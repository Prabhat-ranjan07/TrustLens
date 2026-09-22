package com.marwadiuniversity.trustlens.domain.engine

class SensitiveDataRedactor {
    companion object {
        fun redact(input: String): String {
            var sanitized = input
            val otpRegex = Regex("""(?i)(otp|pin|code|password)\s*[:=]?\s*\d{4,6}""")
            sanitized = otpRegex.replace(sanitized) { match ->
                val prefix = match.value.takeWhile { it.isLetter() || it.isWhitespace() }
                "$prefix [REDACTED]"
            }
            return sanitized
        }
    }
}
