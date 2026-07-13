package io.github.sirmustfailalot.projectash.commands.menu

import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

object ProjectAshMenus {
    fun root(source: CommandSourceStack) {
        val buttons = mutableListOf(MenuButton("Player", "/ProjectAsh Player"))
        if (source.hasPermission(3)) buttons += MenuButton("Server", "/ProjectAsh Server")
        lineMenu(source, "Project Ash", buttons)
    }

    fun player(source: CommandSourceStack) = lineMenu(
        source,
        "Project Ash > Player",
        listOf(
            MenuButton("Back", "/ProjectAsh", colour = ChatFormatting.GRAY),
            MenuButton("Special", "/ProjectAsh Player Special", colour = ChatFormatting.LIGHT_PURPLE),
            MenuButton("CatchEmAll", "/ProjectAsh Player CatchEmAll", colour = ChatFormatting.GOLD)
        )
    )

    fun server(source: CommandSourceStack) = lineMenu(
        source,
        "Project Ash > Server",
        listOf(
            MenuButton("Back", "/ProjectAsh", colour = ChatFormatting.GRAY),
            MenuButton("Discord", "/ProjectAsh Server Discord", colour = ChatFormatting.BLUE),
            MenuButton("Showcase", "/ProjectAsh Server Showcase", colour = ChatFormatting.GREEN),
            MenuButton("InGame", "/ProjectAsh Server InGame", colour = ChatFormatting.AQUA),
            MenuButton("Perfect", "/ProjectAsh Server Perfect", colour = ChatFormatting.LIGHT_PURPLE),
            MenuButton("Shiny", "/ProjectAsh Server Shiny", colour = ChatFormatting.GOLD),
            MenuButton("UnknownSpawns", "/ProjectAsh Server UnknownSpawns", colour = ChatFormatting.DARK_GRAY),
            MenuButton("Label", "/ProjectAsh Server Label", colour = ChatFormatting.YELLOW),
            MenuButton("Special", "/ProjectAsh Server Special", colour = ChatFormatting.LIGHT_PURPLE),
            MenuButton("Blacklist", "/ProjectAsh Server Blacklist", colour = ChatFormatting.DARK_RED)
        )
    )

    fun section(
        source: CommandSourceStack,
        section: MenuSection,
        extraButtons: List<MenuButton> = emptyList()
    ) {
        val buttons = mutableListOf<MenuButton>()
        section.backCommand?.let { buttons += MenuButton("Back", it, colour = ChatFormatting.GRAY) }
        section.actions.forEach { action ->
            val command = "${section.commandPath} ${action.commandName}" +
                if (action.clickMode == MenuClickMode.SUGGEST) " " else ""
            buttons += MenuButton(action.label, command, action.clickMode, action.colour)
        }
        buttons += extraButtons
        lineMenu(source, "Project Ash > ${section.title}", buttons)
    }

    fun custom(source: CommandSourceStack, title: String, buttons: List<MenuButton>) =
        lineMenu(source, title, buttons)

    private fun lineMenu(source: CommandSourceStack, title: String, buttons: List<MenuButton>) {
        var line: Component = Component.literal("$title: ").withStyle(ChatFormatting.GOLD)
        buttons.forEach { line = line.copy().append(buttonComponent(it)) }
        source.sendSuccess({ line }, false)
    }

    private fun buttonComponent(button: MenuButton): Component {
        val action = if (button.clickMode == MenuClickMode.RUN) {
            ClickEvent.Action.RUN_COMMAND
        } else {
            ClickEvent.Action.SUGGEST_COMMAND
        }
        return Component.literal("[${button.label}] ").withStyle { style ->
            style.withColor(button.colour)
                .withClickEvent(ClickEvent(action, button.command))
                .withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.literal(button.command).withStyle(ChatFormatting.GRAY)
                    )
                )
        }
    }
}
