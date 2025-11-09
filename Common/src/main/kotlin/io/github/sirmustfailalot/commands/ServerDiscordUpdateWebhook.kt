package io.github.sirmustfailalot.projectash.commands
import io.github.sirmustfailalot.Config

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import java.net.URI

object ServerDiscordUpdateWebhook : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server updateWebhook <url>
        literal("DiscordWebhook")
            .then(
                argument("url", StringArgumentType.greedyString())
                    .executes { ctx ->
                        val url = StringArgumentType.getString(ctx, "url").trim()
                        validateUrl(url)?.let { throw it.create() }
                        Config.setServerDiscordWebhook(url)
                        ctx.source.sendSuccess(
                            { Component.literal("[Project Ash] Discord webhook updated.") },
                            true
                        )
                        1
                    }
            )

    private fun validateUrl(url: String): SimpleCommandExceptionType? =
        try {
            val u = URI(url)
            val ok = (u.scheme == "http" || u.scheme == "https") && !u.host.isNullOrBlank()
            if (ok) null else SimpleCommandExceptionType(Component.literal("[Project Ash] Invalid URL: $url"))
        } catch (_: Exception) {
            SimpleCommandExceptionType(Component.literal("[Project Ash] Invalid URL: $url"))
        }
}