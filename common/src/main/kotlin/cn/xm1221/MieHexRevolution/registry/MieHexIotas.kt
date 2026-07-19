package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes

object MieHexIotas:Miehex_revolutionRegistrar<IotaType<*>>(
    HexRegistries.IOTA_TYPE,
    { HexIotaTypes.REGISTRY}
) {
    private fun make(name:String,type: IotaType<*>)=register(name){
        type
    }


}