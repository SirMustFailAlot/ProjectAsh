package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import java.net.URI

object ServerDiscord : PAServerSubcommand {
    private val section = MenuSection("Server Discord", "/ProjectAsh Server Discord", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))
    private val webhookSection = MenuSection("Server Discord > Webhook", "/ProjectAsh Server Discord Webhook", "/ProjectAsh Server Discord", listOf(MenuAction.CHECK, MenuAction.ADD, MenuAction.REMOVE))
    private val thumbnailSection = MenuSection("Server Discord > Thumbnails", "/ProjectAsh Server Discord Thumbnails", "/ProjectAsh Server Discord", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Discord")
        .executes { ctx ->
            ProjectAshMenus.section(ctx.source, section, listOf(
                MenuButton("Webhook", "/ProjectAsh Server Discord Webhook", colour = ChatFormatting.BLUE),
                MenuButton("Thumbnails", "/ProjectAsh Server Discord Thumbnails", colour = ChatFormatting.AQUA)
            ))
            1
        }
        .then(literal("Check").executes { ctx ->
            ctx.source.sendSuccess({
                Component.literal("[Project Ash] Discord: ")
                    .append(if (Config.data.server.discordEnabled) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" | Thumbnails: "))
                    .append(if (Config.data.server.discordThumbnails) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" | Webhook: ${if (webhookSet()) "SET" else "NOT SET"}"))
            }, false)
            1
        })
        .then(literal("Enable").executes { ctx -> Config.setServerDiscordEnabled(true); ctx.source.sendSuccess({ prefixedStatus("Discord announcements", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerDiscordEnabled(false); ctx.source.sendSuccess({ prefixedStatus("Discord announcements", false) }, true); 1 })
        .then(webhookCommand())
        .then(thumbnailCommand())

    private fun webhookCommand() = literal("Webhook")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, webhookSection); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord webhook: ${if (webhookSet()) "SET" else "NOT SET"}") }, false); 1 })
        .then(literal("Add").then(argument("url", StringArgumentType.greedyString()).executes { ctx ->
            val url = StringArgumentType.getString(ctx, "url").trim()
            if (!validUrl(url)) { ctx.source.sendFailure(Component.literal("[Project Ash] Invalid webhook URL.")); return@executes 0 }
            Config.setServerDiscordWebhook(url)
            ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord webhook updated.") }, true)
            1
        }))
        .then(literal("Remove").executes { ctx -> Config.setServerDiscordWebhook(""); ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord webhook removed.") }, true); 1 })

    private fun thumbnailCommand() = literal("Thumbnails")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, thumbnailSection); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("Discord thumbnails", Config.data.server.discordThumbnails) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setServerDiscordThumbnails(true); ctx.source.sendSuccess({ prefixedStatus("Discord thumbnails", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerDiscordThumbnails(false); ctx.source.sendSuccess({ prefixedStatus("Discord thumbnails", false) }, true); 1 })

    private fun webhookSet() = Config.data.server.discordWebhook.isNotBlank() && !Config.data.server.discordWebhook.contains("your.webhook.url")
    private fun validUrl(url: String) = try { val uri = URI(url); uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() } catch (_: Exception) { false }
}
