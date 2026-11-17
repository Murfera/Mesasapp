package br.com.mesasapp.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.Config
import br.com.mesasapp.data.preferences.ConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfigUiState(
    val config: Config = Config(),
    val carregado: Boolean = false,
    val mensagem: String? = null
)

class ConfigViewModel(private val configStore: ConfigStore) : ViewModel() {

    private val mensagem = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConfigUiState> = combine(
        configStore.config,
        mensagem
    ) { config, msg ->
        ConfigUiState(config = config, carregado = true, mensagem = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfigUiState())

    fun salvar(config: Config) {
        viewModelScope.launch {
            configStore.salvar(config)
            mensagem.value = "Configuracoes salvas."
        }
    }

    fun mensagemLida() { mensagem.value = null }
}
