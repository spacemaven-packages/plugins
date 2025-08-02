package net.derfruhling.cmake

import org.gradle.internal.file.PathToFileResolver
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal
import org.gradle.nativeplatform.platform.internal.OperatingSystemInternal
import org.gradle.nativeplatform.toolchain.internal.ExtendableToolChain
import org.gradle.nativeplatform.toolchain.internal.NativeLanguage
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider
import org.gradle.nativeplatform.toolchain.internal.UnsupportedPlatformToolProvider

import javax.inject.Inject

class CMakeUnknownToolchain extends ExtendableToolChain<CMakeUnknownPlatformToolchain> {
    @Inject
    CMakeUnknownToolchain(String name, PathToFileResolver fileResolver) {
        super(name, null, OperatingSystem.current(), fileResolver)
    }

    @Override
    protected String getTypeName() {
        return "CMakeUnknown"
    }

    @Override
    PlatformToolProvider select(NativePlatformInternal targetPlatform) {
        return new UnsupportedPlatformToolProvider(OperatingSystem.current() as OperatingSystemInternal, "Cannot use this toolchain for building with Gradle")
    }

    @Override
    PlatformToolProvider select(NativeLanguage sourceLanguage, NativePlatformInternal targetMachine) {
        return new UnsupportedPlatformToolProvider(OperatingSystem.current() as OperatingSystemInternal, "Cannot use this toolchain for building with Gradle")
    }
}
