package br.com.mesasapp.data.seguranca

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.mesasapp.data.preferences.ModoPrivacidade

/**
 * A trava do app (menu > Privacidade).
 *
 * Quem guarda a senha e a digital e o proprio Android: o app nunca ve, nunca
 * grava e nunca compara credencial nenhuma. Ele so pergunta ao sistema "essa
 * pessoa e a dona do aparelho?" e recebe sim ou nao. Isso e deliberado — uma
 * senha propria do app seria mais um segredo para guardar, guardado pior.
 *
 * A trava protege a tela, nao o arquivo: os pedidos ficam na pasta privada do
 * app, que o Android ja isola de outros apps, mas nao sao cifrados. Quem tiver
 * o aparelho destravado e ferramentas de desenvolvedor alcanca o banco. Serve
 * para o celular esquecido na mesa, nao contra pericia.
 */
object Trava {

    /** Sem senha, PIN ou padrao no aparelho nao existe o que pedir. */
    fun telaDeBloqueioConfigurada(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isDeviceSecure
    }

    fun digitalCadastrada(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Por que este modo nao pode ser escolhido agora, ou null se pode.
     *
     * A tela mostra o motivo em vez de so desabilitar a opcao: "nao da" sem
     * explicacao manda o usuario adivinhar.
     */
    fun indisponivelPorque(context: Context, modo: ModoPrivacidade): String? = when {
        modo == ModoPrivacidade.NENHUM -> null

        !telaDeBloqueioConfigurada(context) ->
            "Este aparelho não tem senha, PIN ou padrão de desbloqueio. " +
                "Configure em Ajustes → Segurança e volte aqui."

        modo == ModoPrivacidade.BIOMETRIA && !digitalCadastrada(context) ->
            "Nenhuma digital cadastrada neste aparelho. " +
                "Cadastre em Ajustes → Segurança → Impressão digital e volte aqui."

        else -> null
    }

    /**
     * Pede o desbloqueio ao Android e chama [aoLiberar] so se ele confirmar.
     *
     * A senha do aparelho e sempre aceita, inclusive no modo Digital. Nao e
     * descuido: sensor molhado, dedo cortado ou leitor com defeito nao podem
     * trancar o dono fora dos proprios pedidos, e quem sabe a senha do celular
     * ja tem o aparelho todo de qualquer forma.
     */
    @Suppress("DEPRECATION")
    fun pedir(
        activity: FragmentActivity,
        modo: ModoPrivacidade,
        titulo: String,
        subtitulo: String,
        aoLiberar: () -> Unit,
        aoFalhar: (String) -> Unit
    ) {
        if (modo == ModoPrivacidade.NENHUM) {
            aoLiberar()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    aoLiberar()
                }

                // Desistiu, errou demais ou o leitor falhou. Dedo errado numa
                // tentativa nao cai aqui: o proprio Android deixa tentar de novo.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    aoFalhar(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setConfirmationRequired(false)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(
                        if (modo == ModoPrivacidade.BIOMETRIA) {
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        } else {
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        }
                    )
                } else {
                    // Antes do Android 11 nao da para pedir SO a senha do
                    // aparelho pelo BiometricPrompt: a combinacao
                    // biometria-ou-senha e a unica aceita. Nessas versoes os
                    // dois modos acabam equivalentes, e e por isso que a tela
                    // de Privacidade avisa.
                    setDeviceCredentialAllowed(true)
                }
            }
            .build()

        prompt.authenticate(info)
    }
}
