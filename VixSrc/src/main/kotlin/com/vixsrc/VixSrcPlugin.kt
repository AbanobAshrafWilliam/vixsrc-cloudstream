package com.vixsrc

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class VixSrcPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(VixSrcProvider())
    }
}
