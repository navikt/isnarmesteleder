package no.nav.syfo.application.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory
import org.flywaydb.core.Flyway
import java.sql.Connection

data class DatabaseConfig(
    val jdbcUrl: String,
    val password: String,
    val username: String,
    val poolSize: Int = 2,
)

class Database(
    private val daoConfig: DatabaseConfig
) : DatabaseInterface {

    override val connection: Connection
        get() = dataSource.connection

    private var dataSource: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = daoConfig.jdbcUrl
            username = daoConfig.username
            password = daoConfig.password
            maximumPoolSize = daoConfig.poolSize
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            metricsTrackerFactory = PrometheusMetricsTrackerFactory()
            validate()
        }
    )

    init {
        runFlywayMigrations()
    }

    private fun runFlywayMigrations() = Flyway.configure().run {
        dataSource(
            daoConfig.jdbcUrl,
            daoConfig.username,
            daoConfig.password,
        )
        load().migrate().migrationsExecuted
    }
}

interface DatabaseInterface {
    val connection: Connection
}
