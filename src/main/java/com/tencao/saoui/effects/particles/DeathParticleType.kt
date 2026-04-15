package com.tencao.saoui.effects.particles

import com.mojang.serialization.Codec
import net.minecraft.client.particle.IAnimatedSprite
import net.minecraft.client.particle.IParticleFactory
import net.minecraft.client.particle.Particle
import net.minecraft.client.world.ClientWorld
import net.minecraft.particles.ParticleType

class DeathParticleType : ParticleType<DeathParticleData>(false, DeathParticleData.DESERIALIZER) {

    override fun func_230522_e_(): Codec<DeathParticleData> {
        return DeathParticleData.CODEC
    }

    class DeathParticleFactory : IParticleFactory<DeathParticleData> {
        private lateinit var sprite: IAnimatedSprite

        private constructor() {
            throw UnsupportedOperationException("Use the FlameParticleFactory(IAnimatedSprite sprite) constructor")
        }

        constructor(sprite: IAnimatedSprite) {
            this.sprite = sprite
        }

        override fun makeParticle(
            data: DeathParticleData,
            world: ClientWorld,
            x: Double,
            y: Double,
            z: Double,
            mx: Double,
            my: Double,
            mz: Double
        ): Particle {
            // mx/my/mz = vitesse initiale transmise depuis StaticRenderer.doSpawnDeathParticles
            // isCoreBurst = true si vitesse faible (nuage central), false sinon (burst/micro)
            val speedSq = mx * mx + my * my + mz * mz
            val isCore = speedSq < 0.02   // vitesse faible = particule centrale
            val ret = DeathParticle(
                world,
                x, y, z,
                mx, my, mz,
                data.r, data.g, data.b,
                isCore
            )
            ret.selectSpriteRandomly(sprite)
            return ret
        }
    }
}
