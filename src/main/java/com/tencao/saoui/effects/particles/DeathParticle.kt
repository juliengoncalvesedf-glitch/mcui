package com.tencao.saoui.effects.particles

import net.minecraft.client.particle.IParticleRenderType
import net.minecraft.client.particle.SpriteTexturedParticle
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.world.ClientWorld
import kotlin.math.max

/**
 * Particule de mort style SAO — Version améliorée fidèle à l'anime.
 *
 * Comportement visuel :
 *  ✨ Flash blanc intense au spawn (0–12% de vie)
 *  ✨ Transition blanc → bleu SAO (#4AABFF) (12–40%)
 *  ✨ Bleu SAO stable (40–65%)
 *  ✨ Fade-out progressif quadratique (65–100%)
 *  ✨ Taille : courbe en cloche (grandit puis rétrécit)
 *  ✨ Physique flottante légère (monte doucement)
 *  ✨ Toujours éclairée au maximum (glow dans le noir)
 *  ✨ Rotation 2D + roll
 */
class DeathParticle(
    world: ClientWorld,
    x: Double, y: Double, z: Double,
    r: Float, g: Float, b: Float
) : SpriteTexturedParticle(world, x, y, z, 0.0, 0.0, 0.0) {

    // Couleurs de base SAO (sauvegardées pour le fade et le flash)
    private val baseRed   = r
    private val baseGreen = g
    private val baseBlue  = b

    // Taille de base (sauvegardée pour la courbe en cloche)
    private val baseQuadSize: Float

    // Rotation
    private val rotSpeed: Float

    init {
        // Mouvement initial : explosion légère avec poussée vers le haut
        this.motionX = ((Math.random() * 2.0 - 1.0) * 0.08).toDouble()
        this.motionY = (Math.random() * 0.08 + 0.02).toDouble()  // toujours vers le haut
        this.motionZ = ((Math.random() * 2.0 - 1.0) * 0.08).toDouble()

        // Rotation aléatoire
        this.rotSpeed = (Math.random().toFloat() - 0.5f) * 0.15f
        this.particleAngle = Math.random().toFloat() * (Math.PI.toFloat() * 2f)

        // Couleur initiale = blanc flash
        this.particleRed   = 1.0f
        this.particleGreen = 1.0f
        this.particleBlue  = 1.0f

        // Taille : légère variation aléatoire
        val sizeVariation = 0.7f + rand.nextFloat() * 0.6f
        this.particleScale *= sizeVariation * 1.4f
        this.baseQuadSize = this.particleScale

        // Durée de vie : 18–35 ticks (0.9–1.75s)
        this.maxAge = (18 + (Math.random() * 17).toInt())

        // Collision activée
        this.canCollide = true
    }

    override fun tick() {
        this.prevPosX = this.posX
        this.prevPosY = this.posY
        this.prevPosZ = this.posZ

        // Expiration
        if (this.age++ >= this.maxAge) {
            this.setExpired()
            return
        }

        val lifeProgress = this.age.toFloat() / this.maxAge.toFloat()

        // ── Couleur & Alpha ───────────────────────────────────────────
        when {
            lifeProgress < 0.12f -> {
                // Flash blanc intense au spawn
                val t = lifeProgress / 0.12f
                this.particleRed   = 1.0f
                this.particleGreen = 1.0f
                this.particleBlue  = 1.0f
                // Alpha : commence à 1.0, reste à 1.0
            }
            lifeProgress < 0.40f -> {
                // Transition blanc → bleu SAO
                val t = (lifeProgress - 0.12f) / 0.28f
                this.particleRed   = 1.0f * (1f - t) + baseRed   * t
                this.particleGreen = 1.0f * (1f - t) + baseGreen * t
                this.particleBlue  = 1.0f * (1f - t) + baseBlue  * t
            }
            lifeProgress < 0.65f -> {
                // Bleu SAO pur et stable
                this.particleRed   = baseRed
                this.particleGreen = baseGreen
                this.particleBlue  = baseBlue
            }
            else -> {
                // Fade-out : bleu SAO qui disparaît progressivement
                val fadeT = (lifeProgress - 0.65f) / 0.35f
                this.particleRed   = baseRed
                this.particleGreen = baseGreen
                this.particleBlue  = baseBlue
                // L'alpha est géré via particleAlpha
                this.particleAlpha = max(0f, 1f - fadeT * fadeT)
            }
        }
        // Reset alpha si pas en fade-out
        if (lifeProgress < 0.65f) this.particleAlpha = 1.0f

        // ── Taille : courbe en cloche ─────────────────────────────────
        val sizeFactor = when {
            lifeProgress < 0.20f -> lifeProgress / 0.20f            // grandit
            lifeProgress < 0.65f -> 1.0f                             // plateau
            else -> 1f - (lifeProgress - 0.65f) / 0.35f             // rétrécit
        }
        this.particleScale = baseQuadSize * (0.2f + 0.8f * sizeFactor)

        // ── Rotation ──────────────────────────────────────────────────
        this.prevParticleAngle = this.particleAngle
        this.particleAngle += Math.PI.toFloat() * rotSpeed * 2.0f

        // ── Physique flottante ────────────────────────────────────────
        // Gravité très douce — monte puis descend légèrement
        if (lifeProgress < 0.5f) {
            this.motionY += 0.002   // légère poussée vers le haut
        } else {
            this.motionY -= 0.001   // retombe doucement
        }

        this.move(this.motionX, this.motionY, this.motionZ)

        // Friction plus douce que l'original (0.9 → 0.92) = flotte plus longtemps
        this.motionX *= 0.92
        this.motionY *= 0.94
        this.motionZ *= 0.92

        if (this.onGround) {
            this.motionX *= 0.6
            this.motionZ *= 0.6
            this.prevParticleAngle = this.particleAngle
            this.particleAngle = 0.0f
        }
    }

    // Toujours éclairée au maximum → glow même dans le noir
    override fun getBrightnessForRender(partialTick: Float): Int {
        return LightTexture.packLight(15, 15)
    }

    override fun getRenderType(): IParticleRenderType {
        // PARTICLE_SHEET_TRANSLUCENT pour supporter l'alpha (fade-out)
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    companion object {
        val randomMotion get() = ((Math.random() * 2.0 - 1.0).toFloat() * 0.05f).toDouble()
    }
}
