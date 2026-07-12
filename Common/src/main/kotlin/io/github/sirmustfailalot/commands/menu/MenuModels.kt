package io.github.sirmustfailalot.projectash.commands.menu

import net.minecraft.ChatFormatting

enum class MenuClickMode { RUN, SUGGEST }

data class MenuButton(
    val label: String,
    val command: String,
    val clickMode: MenuClickMode = MenuClickMode.RUN,
    val colour: ChatFormatting = ChatFormatting.AQUA
)

enum class MenuAction(
    val label: String,
    val commandName: String,
    val clickMode: MenuClickMode = MenuClickMode.RUN,
    val colour: ChatFormatting
) {
    CHECK("Check", "Check", colour = ChatFormatting.YELLOW),
    ADD("Add", "Add", MenuClickMode.SUGGEST, ChatFormatting.GREEN),
    REMOVE("Remove", "Remove", MenuClickMode.SUGGEST, ChatFormatting.RED),
    CLEAR("Clear", "Clear", colour = ChatFormatting.DARK_RED),
    ENABLE("Enable", "Enable", colour = ChatFormatting.GREEN),
    DISABLE("Disable", "Disable", colour = ChatFormatting.RED)
}

data class MenuSection(
    val title: String,
    val commandPath: String,
    val backCommand: String? = null,
    val actions: List<MenuAction>
)
