package io.github.monun.psychics.ability.wire

import io.github.monun.psychics.*
import io.github.monun.psychics.util.TargetFilter
import io.github.monun.tap.config.Config
import io.github.monun.tap.config.Name
import io.github.monun.tap.fake.FakeEntity
import io.github.monun.tap.fake.Movement
import io.github.monun.tap.fake.Trail
import io.github.monun.tap.math.normalizeAndLength
import io.github.monun.tap.trail.TrailSupport
import net.kyori.adventure.text.Component.text
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack

@Name("wire")
class AbilityConceptWire : AbilityConcept() {
    @Config
    var wireSpeed = 5.0

    init {
        displayName = "갈고리"
        description = listOf(
            text("지형에 걸리는 갈고리를 발사해 자신을 지형으로 끌어당깁니다."),
        )

        type = AbilityType.ACTIVE
        wand = ItemStack(Material.ARROW)
        range = 256.0
        cooldownTime = 6000L
        durationTime = 4000L
    }
}

class AbilityWire : Ability<AbilityConceptWire>() {
    private var hitWire: FakeEntity<ArmorStand>? = null
    private var initDistance: Double? = null
    private var leftClicked: Boolean = false

    override fun onEnable() {
        psychic.registerEvents(WireListener())
        psychic.runTaskTimer(this::tick, 0L, 1L)
    }

    override fun onDisable() {
        resetVar()
    }

    override fun test(): TestResult {
        if (psychic.channeling != null)
            return TestResult.FailedChannel

        return super.test()
    }

    private fun tick() {
        hitWire?.let { wire ->
            if (durationTime <= 0L) {
                removeWire(wire)

                return
            }

            if (initDistance == null) return
            if (!leftClicked) return

            val player = esper.player
            val playerLocation = player.location
            val wireLocation = wire.location
            val distance = playerLocation.distance(wireLocation)

            if (distance <= 3.0) {
                removeWire(wire)

                cooldownTime = 0L

                return
            }

            val distanceRate = initDistance!! / distance
            val speedRate = concept.range / initDistance!!

            val velocity = wireLocation.toVector().subtract(playerLocation.toVector())
                .multiply(distanceRate * speedRate * 0.001 * concept.wireSpeed)

            player.velocity = velocity
        }
    }

    private fun resetVar() {
        hitWire = null
        initDistance = null
        leftClicked = false
    }

    private fun removeWire(wire: FakeEntity<ArmorStand>) {
        resetVar()

        wire.remove()

        val wireLocation = wire.location
        val world = wireLocation.world
        world.spawnParticle(Particle.CLOUD, wireLocation.clone().apply { y += 1.62 }, 32, 0.0, 0.0, 0.0, 0.1)
        world.playSound(wireLocation, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.MASTER, 2.0F, 2.0F)
    }

    inner class WireListener : Listener {
        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if (event.item?.type != concept.wand?.type) return

            val action = event.action

            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                val result = test()

                if (result != TestResult.Success) {
                    result.message(this@AbilityWire)?.let { esper.player.sendActionBar(it) }

                    return
                }

                cooldownTime = concept.cooldownTime

                val player = esper.player
                val location = player.location
                val eyeLocation = player.eyeLocation
                val world = player.world

                val projectile = WireProjectile().apply {
                    wire = this@AbilityWire.psychic.spawnFakeEntity(
                        eyeLocation.clone().apply { y -= 1.62 },
                        ArmorStand::class.java
                    ).apply {
                        velocity = eyeLocation.direction.multiply(concept.wireSpeed)

                        updateMetadata {
                            isVisible = false
                            isMarker = true
                        }

                        updateEquipment {
                            helmet = ItemStack(Material.IRON_BLOCK)
                        }
                    }
                }

                psychic.launchProjectile(eyeLocation, projectile)

                world.playSound(location, Sound.ITEM_CROSSBOW_SHOOT, 2.0F, 1.5F)
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                hitWire?.let { wire ->
                    initDistance = esper.player.location.distance(wire.location)
                    leftClicked = true
                }
            }
        }

        @EventHandler
        fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
            if (!event.isSneaking) return

            hitWire?.let { wire ->
                removeWire(wire)
            }
        }
    }

    inner class WireProjectile : PsychicProjectile(1200, concept.range) {
        internal var wire: FakeEntity<ArmorStand>? = null

        override fun onMove(movement: Movement) {
            wire?.moveTo(movement.to.clone().apply { y -= 1.62 })
        }

        override fun onTrail(trail: Trail) {
            trail.velocity?.let { v ->
                val from = trail.from
                val world = from.world

                TrailSupport.trail(from, trail.to, 0.1) { w, x, y, z ->
                    w.spawnParticle(Particle.CRIT, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
                }

                val length = v.normalizeAndLength()

                if (length == 0.0) return

                val filter = TargetFilter(esper.player)

                world.rayTrace(
                    from,
                    v,
                    length,
                    FluidCollisionMode.NEVER,
                    true,
                    1.0,
                    filter
                )?.let { result ->
                    durationTime = concept.durationTime

                    val hitLocation = result.hitPosition.toLocation(world)

                    wire?.let { wire ->
                        this.wire = null

                        wire.moveTo(hitLocation.clone().apply { y -= 1.62 })

                        hitWire = wire
                    }

                    remove()
                }
            }
        }

        override fun onRemove() {
            wire?.remove()
        }
    }
}
