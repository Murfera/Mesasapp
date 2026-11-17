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
    version = 2,
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
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    // ATENCAO: enquanto o app nao foi publicado, mudanca de schema
                    // recria o banco em vez de migrar. **Antes da primeira versao na
                    // Play Store isto tem que virar migracao de verdade**, senao uma
                    // atualizacao apaga os pedidos de quem ja usa.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
