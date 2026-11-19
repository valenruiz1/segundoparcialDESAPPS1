package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "estado_programa")
data class EstadoPrograma(
    @PrimaryKey val id: Int = 1,
    val descargaExitosa: Boolean = true
)

@Dao
interface CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    fun observar(): Flow<List<Charla>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fichas: List<Charla>)

    @Query("DELETE FROM charlas")
    suspend fun borrarTodas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEstado(estado: EstadoPrograma)

    @Query("SELECT EXISTS(SELECT 1 FROM estado_programa WHERE id = 1 AND descargaExitosa = 1)")
    fun observarDescargaExitosa(): Flow<Boolean>

    @Transaction
    suspend fun reemplazarInstantanea(fichas: List<Charla>) {
        borrarTodas()
        if (fichas.isNotEmpty()) guardar(fichas)
        guardarEstado(EstadoPrograma())
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, EstadoPrograma::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `estado_programa` " +
                "(`id` INTEGER NOT NULL, `descargaExitosa` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        database.execSQL(
            "INSERT INTO `estado_programa` (`id`, `descargaExitosa`) " +
                "SELECT 1, 1 WHERE EXISTS (SELECT 1 FROM `charlas`)"
        )
    }
}
