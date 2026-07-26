package cn.xm1221.MieHexRevolution.block.idea

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class IdeaBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(type, pos, state) {

    var material: BlockState = Blocks.AIR.defaultBlockState()
        private set

    val hasCustomMaterial: Boolean get() = !material.isAir

    fun setMaterial(state: BlockState) {
        material = state
        setChanged()
        if (level != null && !level!!.isClientSide) {
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        }
    }

    fun resetMaterial() {
        material = Blocks.AIR.defaultBlockState()
        setChanged()
        if (level != null && !level!!.isClientSide) {
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        }
    }

    fun cycleMaterial(): Boolean {
        return tryCycleProperty(material)?.let {
            material = it
            setChanged()
            if (level != null && !level!!.isClientSide) {
                level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
            }
            true
        } ?: false
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Material")) {
            material = NbtUtils.readBlockState(registries, tag.getCompound("Material"))
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Material", NbtUtils.writeBlockState(material))
    }

    companion object {
        private fun tryCycleProperty(state: BlockState): BlockState? {
            for (prop in state.properties) {
                if (prop.name in setOf("facing", "axis", "half", "open")) {
                    return state.cycle(prop)
                }
            }
            return null
        }
    }
}
