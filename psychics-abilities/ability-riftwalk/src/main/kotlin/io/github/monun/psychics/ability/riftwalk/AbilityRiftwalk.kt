package io.github.monun.psychics.ability.riftwalk

import io.github.monun.psychics.AbilityConcept
import io.github.monun.psychics.ActiveAbility
import io.github.monun.psychics.attribute.EsperAttribute
import io.github.monun.psychics.damage.Damage
import io.github.monun.psychics.damage.DamageType
import io.github.monun.tap.config.Name
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

@Name("riftwalk")
class AbilityConceptRiftwalk : AbilityConcept() {
    init {
        displayName = "균열 이동"
        description = listOf(
            text("지정한 방향으로 순간이동하며 피해를 입힙니다.")
        )

        wand = ItemStack(Material.DIAMOND_SWORD)
        cost = 10.0
        range = 8.0
        cooldownTime = 1000L
        castingTime = 250L
        damage = Damage.of(DamageType.MELEE, EsperAttribute.ATTACK_DAMAGE to 2.0)
    }
}

class AbilityRiftwalk : ActiveAbility<AbilityConceptRiftwalk>(), Listener {
    override fun onEnable() {
        psychic.registerEvents(this)
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val concept = concept
        val player = esper.player

        val direction = player.location.toVector()
        val destination = direction.normalize().multiply(concept.range).toLocation(player.world)

        player.teleport(destination)

        player.world.getNearbyLivingEntities(destination, 1.5).forEach {
            if (it == player) {
                return@forEach
            }

            it.psychicDamage()
        }
    }
}
