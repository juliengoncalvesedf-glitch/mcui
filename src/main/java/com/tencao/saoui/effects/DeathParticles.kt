package com.tencao.saoui.effects.particles

import net.minecraft.client.particle.IParticleRenderType
import net.minecraft.client.particle.SpriteTexturedParticle
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.world.ClientWorld
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.PI

/**
 * Particule de mort style SAO — Éclats cristallins fidèles à l'anime.
 *
 * Comportement visuel :
 *  🔺 Texture triangulaire (éclat de verre/cristal) parmi 4 variantes
 *  💥 Explosion violente initiale : burst rapide dans toutes les directions
 *  ✨ Flash blanc intense au spawn (0–15% de vie)
 *  🔵 Transition vers bleu SAO électrique (15–40%)
 *  🌊 Flottement progressif + décélération naturelle (physique réaliste)
 *  ⬆️  Légère lévitation persistante (les éclats montent doucement)
 *  🌀 Rotation continue (chaque éclat tourne sur lui-même)
 *  💡 Toujours éclairée au maximum (glow dans le noir)
 *  🌟 Fade-out quadratique sur les 35% finaux
 */
class DeathParticle(
    world: ClientWorld,
    x: Double, y: Double, z: Double,
    vx: Double, vy: Double, vz: Double,
    r: Float, g: Float, b: Float,
    private val isCoreBurst: Boolean = false
) : SpriteTexturedParticle(world, x, y, z, 0.0, 0.0, 0.0) {

    private val baseRed   = r
    private val baseGreen = g
    private val baseBlue  = b
    private val baseQuadSize: Float
    private val rotSpeed: Float

    init {
        // ── Vitesse initiale : explosion violente ─────────────────────────
        this.motionX = vx
        this.motionY = vy
        this.motionZ = vz

        // ── Rotation aléatoire rapide ─────────────────────────────────────
        this.rotSpeed = ((Math.random() - 0.5) * 0.25).toFloat()
        this.particleAngle = (Math.random() * 2.0 * PI).toFloat()

        // ── Couleur initiale = blanc flash ────────────────────────────────
        this.particleRed   = 1.0f
        this.particleGreen = 1.0f
        this.particleBlue  = 1.0f
        this.particleAlpha = 1.0f

        // ── Taille : grande variation selon le type ───────────────────────
        val sizeBase = if (isCoreBurst) {
            // Particules centrales : plus grandes (les gros éclats)
            1.5f + rand.nextFloat() * 1.2f
        } else {
            // Particules normales : taille variée pour le côté naturel
            0.5f + rand.nextFloat() * 1.8f
        }
        this.particleScale *= sizeBase
        this.baseQuadSize = this.particleScale

        // ── Durée de vie : 20–45 ticks ────────────────────────────────────
        this.maxAge = if (isCoreBurst) {
            25 + (Math.random() * 15).toInt()   // burst central plus court
        } else {
            20 + (Math.random() * 25).toInt()   // fragments périphériques
        }

        this.canCollide = true
    }

    override fun tick() {
        this.prevPosX = this.posX
        this.prevPosY = this.posY
        this.prevPosZ = this.posZ

        if (this.age++ >= this.maxAge) {
            this.setExpired()
            return
        }

        val life = this.age.toFloat() / this.maxAge.toFloat()

        // ── Couleur & Alpha ───────────────────────────────────────────────
        when {
            life < 0.15f -> {
                // Flash blanc pur : très brillant au spawn
                this.particleRed   = 1.0f
                this.particleGreen = 1.0f
                this.particleBlue  = 1.0f
                this.particleAlpha = 1.0f
            }
            life < 0.40f -> {
                // Transition blanc → bleu SAO électrique
                val t = (life - 0.15f) / 0.25f
                this.particleRed   = 1.0f - t * (1.0f - baseRed)
                this.particleGreen = 1.0f - t * (1.0f - baseGreen)
                this.particleBlue  = 1.0f   // reste à 1.0 (bleu maximal)
                this.particleAlpha = 1.0f
            }
            life < 0.65f -> {
                // Bleu SAO pur et stable
                this.particleRed   = baseRed
                this.particleGreen = baseGreen
                this.particleBlue  = baseBlue
                this.particleAlpha = 1.0f
            }
            else -> {
                // Fade-out quadratique
                val fadeT = (life - 0.65f) / 0.35f
                this.particleRed   = baseRed
                this.particleGreen = baseGreen
                this.particleBlue  = baseBlue
                this.particleAlpha = max(0f, 1f - fadeT * fadeT)
            }
        }

        // ── Taille : cloche asymétrique ───────────────────────────────────
        // Grandit rapidement au début (burst), plateau, rétrécit à la fin
        val sizeFactor = when {
            life < 0.15f -> 0.4f + life / 0.15f * 0.6f   // grandit vite : 0.4→1.0
            life < 0.60f -> 1.0f                           // plateau stable
            else -> {
                val t = (life - 0.60f) / 0.40f
                1.0f - t * t                                // rétrécit en courbe
            }
        }
        this.particleScale = baseQuadSize * sizeFactor

        // ── Rotation continue ─────────────────────────────────────────────
        this.prevParticleAngle = this.particleAngle
        this.particleAngle += (PI * rotSpeed * 2.0).toFloat()

        // ── Physique : lévitation + décélération ──────────────────────────
        // Phase active : décélération du burst initial + légère lévitation
        if (life < 0.5f) {
            this.motionY += 0.0015  // micro-poussée vers le haut
        } else {
            this.motionY -= 0.0008  // gravité légère sur la fin
        }

        this.move(this.motionX, this.motionY, this.motionZ)

        // Friction : lente pour que les éclats glissent longtemps dans l'air
        this.motionX *= 0.91
        this.motionY *= 0.95
        this.motionZ *= 0.91

        if (this.onGround) {
            this.motionX *= 0.55
            this.motionZ *= 0.55
            this.prevParticleAngle = this.particleAngle
            this.particleAngle = 0.0f
        }
    }

    // Glow maximal : visible même dans les zones sombres
    override fun getBrightnessForRender(partialTick: Float): Int {
        return LightTexture.packLight(15, 15)
    }

    override fun getRenderType(): IParticleRenderType {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }
}
