# Room gera implementacoes via reflection no nome da classe do banco.
-keep class br.com.mesasapp.data.local.MesasappDatabase_Impl { *; }

# Mantem os nomes dos enums persistidos pelo Room.
-keepclassmembers enum br.com.mesasapp.** { *; }
