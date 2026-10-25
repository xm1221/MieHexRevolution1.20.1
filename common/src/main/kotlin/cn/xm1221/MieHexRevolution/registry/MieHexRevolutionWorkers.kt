package cn.xm1221.MieHexRevolution.registry

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.casting.actions.imports.worker.ImportWorker
import cn.xm1221.MieHexRevolution.casting.actions.useful.envs.worker.RemoteCastEnvWorker
import cn.xm1221.MieLib.registry.CastingVMHook

object MieHexRevolutionWorkers {
    fun init() {
        CastingVMHook.register(Miehex_revolution.id("remote_cast_env"), RemoteCastEnvWorker)
        CastingVMHook.register(Miehex_revolution.id("import"), ImportWorker)
    }
}
