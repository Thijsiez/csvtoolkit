package ch.icken.csvtoolkit.util

enum class OperatingSystem {
    MACOS, WINDOWS, LINUX, OTHER
}

val CurrentOS by lazy {
    with(System.getProperty("os.name").lowercase()) {
        when {
            contains("mac") || contains("darwin") -> OperatingSystem.MACOS
            contains("win") -> OperatingSystem.WINDOWS
            contains("nux") || contains("nix") || contains("aix") -> OperatingSystem.LINUX
            else -> OperatingSystem.OTHER
        }
    }
}

val onMac by lazy { CurrentOS == OperatingSystem.MACOS }