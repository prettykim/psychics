package io.github.monun.psychics.ability.riftwalk

import io.github.monun.psychics.AbilityConcept
import io.github.monun.psychics.ActiveAbility
import io.github.monun.psychics.TestResult
import io.github.monun.psychics.attribute.EsperAttribute
import io.github.monun.psychics.damage.Damage
import io.github.monun.psychics.damage.DamageType
import io.github.monun.tap.config.Config
import io.github.monun.tap.config.Name
import net.kyori.adventure.text.Component.text
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

@Name("riftwalk")
class AbilityConceptRiftwalk : AbilityConcept() {
    @Config
    var radius = 3.0

    init {
        displayName = "균열 이동"
        description = listOf(
            text("근처로 순간이동하며 주변에 있는 적에게 피해를 입힙니다.")
        )

        wand = ItemStack(Material.AMETHYST_SHARD)
        cost = 10.0
        range = 8.0
        cooldownTime = 2000L
        castingTime = 250L
        damage = Damage.of(DamageType.MELEE, EsperAttribute.ATTACK_DAMAGE to 4.0)
    }
}

class AbilityRiftwalk : ActiveAbility<AbilityConceptRiftwalk>(), Listener {
    override fun onEnable() {
        psychic.registerEvents(this)
    }

    override fun tryCast(
        event: PlayerEvent,
        action: WandAction,
        castingTime: Long,
        cost: Double,
        targeter: (() -> Any?)?
    ): TestResult {
        if (action != WandAction.RIGHT_CLICK) {
            return TestResult.FailedAction
        }

        return super.tryCast(event, action, castingTime, cost, targeter)
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val concept = concept

        psychic.consumeMana(concept.cost)
        cooldownTime = concept.cooldownTime

        val player = esper.player

        val start = player.location
        val end = start.clone().add(start.direction.multiply(concept.range))
        val world = player.world

        val destination = safeTeleport(player, start, end)

        world.spawnParticle(
            Particle.DRAGON_BREATH,
            destination.clone().apply { y += 1 },
            64,
            0.0,
            0.0,
            0.0,
            0.1,
        )
        world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 2.0F, 1.0F)

        world.getNearbyLivingEntities(destination, concept.radius).forEach {
            if (it.uniqueId == player.uniqueId) {
                return@forEach
            }

            it.psychicDamage()
        }
    }

    // TODO: Refactoring
    private fun safeTeleport(player: Player, start: Location, end: Location): Location {
        val obstruction = isPathObstructed(start, end)

        var destination = end

        if (obstruction == -1) {
            player.teleport(destination)

            return destination
        }

        destination = start.clone().add(player.location.direction.multiply(obstruction))

        if (start.world.getBlockAt(destination).type.isSolid) {
            destination.add(0.0, 1.0, 0.0)
            player.teleport(destination)
        } else {
            player.teleport(destination)
        }

        return destination
    }

    // TODO: Refactoring
    private fun isPathObstructed(start: Location, end: Location): Int {
        val vector = end.toVector().subtract(start.toVector())
        val unitVector = vector.clone().normalize()

        (0..vector.length().toInt()).forEach { i ->
            val block = start.world.getBlockAt(
                start.toVector().add(unitVector.clone().multiply(i)).toLocation(start.world)
            )

            if (block.type.isSolid) {
                return i
            }
        }

        return -1
    }
}
