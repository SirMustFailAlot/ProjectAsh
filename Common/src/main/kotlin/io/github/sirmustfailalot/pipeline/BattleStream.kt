package io.github.sirmustfailalot.projectash.pipeline

object BattleStream {

    data class BattleGlance(
        val isPvP: Boolean,
        val playerNames: String,
        val opponentNames: String
    )

    data class BattleSummaryGlance(
        val participants: List<BattleParticipantSnapshot>
    )

    data class BattleParticipantSnapshot(
        val displayName: String,
        val isWinner: Boolean,
        val teams: List<BattlePokemonSnapshot>
    )

    data class BattlePokemonSnapshot(
        val name: String,
        val level: Int,
        val isShiny: Boolean,
        val isFainted: Boolean
    )
}