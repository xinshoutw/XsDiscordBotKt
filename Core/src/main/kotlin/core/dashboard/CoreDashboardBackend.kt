package tw.xinshou.discord.core.dashboard

import org.slf4j.LoggerFactory

/**
 * Dashboard server stub. Real implementation lives in WebDashboard module.
 * This provides the interface for Core to start/stop the dashboard lifecycle.
 */
class DashboardServer {
    private val logger = LoggerFactory.getLogger(DashboardServer::class.java)

    fun start() {
        logger.info("Dashboard server placeholder (real impl in WebDashboard module)")
    }

    fun stop() {
        // no-op
    }
}
