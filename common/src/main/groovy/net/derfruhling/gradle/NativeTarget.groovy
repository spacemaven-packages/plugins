package net.derfruhling.gradle

import org.gradle.nativeplatform.TargetMachine
import org.gradle.nativeplatform.TargetMachineFactory
import org.gradle.nativeplatform.platform.NativePlatform

import java.util.function.Function

enum NativeTarget implements Serializable {
    LINUX_X64("linux", "x86-64", { it.linux.x86_64 }),
    MACOS_X64("macos", "x86-64", { it.macOS.x86_64 }) {
        @Override
        Map<String, String> getExtraCMakeArgs() {
            return [CMAKE_OSX_ARCHITECTURES: 'x86_64']
        }
    },
    MACOS_AARCH64("macos", "aarch64", { it.macOS.architecture("aarch64") }) {
        @Override
        Map<String, String> getExtraCMakeArgs() {
            return [CMAKE_OSX_ARCHITECTURES: 'arm64']
        }
    },
    WINDOWS_X64("windows", "x86-64", { it.windows.x86_64 })

    final String platform, architecture, variantName
    private final Function<TargetMachineFactory, TargetMachine> build
    private TargetMachine machine = null

    private NativeTarget(String platform, String architecture, Function<TargetMachineFactory, TargetMachine> build) {
        this.platform = platform
        this.architecture = architecture
        this.build = build
        this.variantName = "${platform}${architecture.replace('-', '_').capitalize()}"
    }

    Map<String, String> getExtraCMakeArgs() { return [:] }

    TargetMachine getMachine() {
        return machine
    }

    void build(TargetMachineFactory factory) {
        if(machine == null) machine = build.apply(factory)
    }

    static NativeTarget getCurrent() {
        def osName = System.getProperty('os.name').toLowerCase()

        if(osName.contains('windows')) {
            return WINDOWS_X64
        } else if(osName.contains('mac')) {
            if(System.getProperty('os.arch').toLowerCase().contains('aarch64')) {
                return MACOS_AARCH64
            } else {
                return MACOS_X64
            }
        } else if(osName.contains('nux')) {
            return LINUX_X64
        } else {
            throw new IllegalStateException("Unsupported platform: $osName")
        }
    }

    static NativeTarget forNativePlatform(NativePlatform platform) {
        return values().find { it.platform == platform.operatingSystem.name && it.architecture == platform.architecture.name }
    }

    boolean isCurrent() {
        return this == getCurrent()
    }

    boolean isCompatible() {
        // macOS platforms can easily build both arm and x86_64 architectures
        return isCurrent() || (this.platform == 'macos' && getCurrent().platform == 'macos' && (this.architecture == 'x86-64' || this.architecture == 'aarch64'))
    }
}
