package br.com.mesasapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ItemEntity::class,
        ComboEntity::class,
        ClienteEntity::class,
        LocacaoEntity::class,
        LocacaoLinhaEntity::class,
        LocacaoPecaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MesasappDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun comboDao(): ComboDao
    abstract fun clienteDao(): ClienteDao
    abstract fun locacaoDao(): LocacaoDao

    companion object {
        @Volatile private var instancia: MesasappDatabase? = null

        fun obter(context: Context): MesasappDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    MesasappDatabase::class.java,
                    "mesasapp.db"
                )
                    // Chave estrangeira das linhas/pecas so vale com isso ligado.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instancia = it }
            }
    }
}
