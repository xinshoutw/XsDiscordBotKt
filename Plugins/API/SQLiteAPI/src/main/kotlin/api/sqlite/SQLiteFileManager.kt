package tw.xinshou.discord.plugin.api.sqlite

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.management.openmbean.KeyAlreadyExistsException

object SQLiteFileManager {
    private val dbConn: MutableMap<String, PathConnection> = HashMap()

    fun getConnection(uniqueKey: String): Connection? = dbConn[uniqueKey]?.connection

    @Synchronized
    fun connect(uniqueKey: String, file: File): Connection {
        if (uniqueKey in dbConn) {
            throw KeyAlreadyExistsException("Connection key $uniqueKey already exists.")
        }

        val connection = DriverManager.getConnection("jdbc:sqlite:${file.canonicalPath}")
        dbConn[uniqueKey] = PathConnection(file.canonicalPath, connection)

        return connection
    }

    @Throws(NoSuchElementException::class)
    @Synchronized
    fun disconnect(uniqueKey: String) {
        dbConn[uniqueKey]?.let {
            it.connection.close()
            dbConn.remove(uniqueKey)
        } ?: throw NoSuchElementException("No connection associated with key: $uniqueKey")
    }

    @Synchronized
    fun deleteDatabase(uniqueKey: String) {
        dbConn[uniqueKey]?.filePath?.let { path ->
            disconnect(uniqueKey)
            File(path).delete()
        }
    }

    private data class PathConnection(
        val filePath: String,
        val connection: Connection,
    )
}
