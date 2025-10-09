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

object DiscordUpdateWebook : PASubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("DiscordWebhookUpdate")
            .requires { it.hasPermission(3) } // OPs only
            .then(
                argument("url", StringArgumentType.greedyString())
                    .executes { ctx ->
                        val url = StringArgumentType.getString(ctx, "url").trim()
                        // validate the URL
                        validateUrl(url)?.let { throw it.create() }

                        Config.setDiscordWebhook(url)

                        ctx.source.sendSuccess(
                            { Component.literal("Discord Webhook URL updated to: $url") },
                            true
                        )
                        1
                    }
            )

    // Simple URL validator
    private fun validateUrl(url: String): SimpleCommandExceptionType? {
        return try {
            val u = URI(url)
            val valid = (u.scheme == "http" || u.scheme == "https") && !u.host.isNullOrBlank()
            if (valid) null else SimpleCommandExceptionType(Component.literal("Invalid URL: $url"))
        } catch (_: Exception) {
            SimpleCommandExceptionType(Component.literal("Invalid URL: $url"))
        }
    }
}