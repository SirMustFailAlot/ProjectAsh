package io.github.sirmustfailalot.platform

import io.github.sirmustfailalot.platform.services.PlatformHelper
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLLoader

class ForgePlatformHelper : PlatformHelper {

    override val platformName = "Forge"

    override fun isModLoaded(modId: String) = ModList.get().isLoaded(modId)

    override val isDevelopmentEnvironment: Boolean
        by lazy { !FMLLoader.isProduction() }
}