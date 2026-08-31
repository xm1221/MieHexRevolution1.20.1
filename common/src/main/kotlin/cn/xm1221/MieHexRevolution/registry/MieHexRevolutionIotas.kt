package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

object MieHexRevolutionIotas:Miehex_revolutionRegistrar<IotaType<*>>(
    HexRegistries.IOTA_TYPE,
    { HexIotaTypes.REGISTRY}
) {
    val IMPORTS = make("imports", ImportsIota.Type)

    private fun make(name:String,type: IotaType<*>)=register(name){
        type
    }


}