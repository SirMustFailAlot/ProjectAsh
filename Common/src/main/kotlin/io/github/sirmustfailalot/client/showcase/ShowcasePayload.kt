package io.github.sirmustfailalot.projectash.showcase

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

data class ShowcasePayload(val itemStack: ItemStack) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ShowcasePayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<ShowcasePayload> = CustomPacketPayload.Type(
            ResourceLocation.parse("projectash:showcase_packet")
        )

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ShowcasePayload> = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            { payload -> payload.itemStack },
            ::ShowcasePayload
        )
    }
}