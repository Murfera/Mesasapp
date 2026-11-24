package br.com.mesasapp.data.seguranca

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.mesasapp.data.preferences.ModoPrivacidade

/**
 * A trava do app (menu > Privacidade). Copia do `ui/lock/AppLock.kt` do Finapp,
 * para as duas funcionarem igual.
 *
 * Quem guarda a senha e a digital e o proprio Android: o app nunca ve, nunca
 * grava e nunca compara credencial nenhuma. Ele so pergunta ao sistema "essa
 * pessoa e a dona do aparelho?" e recebe sim ou nao.
 *
 * A trava protege a tela, nao o arquivo: os pedidos ficam na pasta privada do
 * app, que o Android ja isola de outros apps, mas nao sao cifrados.
 */
object Trava {

    /** Digital cadastrada no aparelho. */
    fun temDigital(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** Senha, PIN ou padrao configurados no celular. */
    fun temSenhaDoCelular(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    fun acharActivity(context: Context): FragmentActivity? {
        var atual = context
        while (atual is ContextWrapper) {
            if (atual is FragmentActivity) return atual
            atual = atual.baseContext
        }
        return null
    }

    /**
     * Pede ao Android a identificacao do modo escolhido; [aoPassar] so roda se passar.
     * Cancelar ou errar nao faz nada: o Android cuida das tentativas e dos bloqueios.
     */
    fun autenticar(activity: FragmentActivity, modo: ModoPrivacidade, titulo: String, aoPassar: () -> Unit) {
        when (modo) {
            ModoPrivacidade.NENHUM -> aoPassar()
            ModoPrivacidade.BIOMETRIA -> activity.perguntar(titulo, BIOMETRIC_WEAK, aoPassar)
            ModoPrivacidade.DISPOSITIVO ->
                // DEVICE_CREDENTIAL sozinho no BiometricPrompt so existe a partir do Android 11.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.perguntar(titulo, DEVICE_CREDENTIAL, aoPassar)
                } else {
                    activity.confirmarSenhaDoCelular(titulo, aoPassar)
                }
        }
    }

    private fun FragmentActivity.perguntar(titulo: String, autenticadores: Int, aoPassar: () -> Unit) {
        val retorno = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                aoPassar()
            }
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), retorno)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setAllowedAuthenticators(autenticadores)
            .apply {
                // O botao de cancelar e obrigatorio quando nao ha senha do aparelho como saida.
                if (autenticadores == BIOMETRIC_WEAK) setNegativeButtonText("Cancelar")
            }
            .build()
        prompt.authenticate(info)
    }

    /** Caminho do Android 8 ao 10: a tela de senha do sistema devolve um resultado. */
    @Suppress("DEPRECATION")
    private fun FragmentActivity.confirmarSenhaDoCelular(titulo: String, aoPassar: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java) ?: return
        val intent = keyguard.createConfirmDeviceCredentialIntent(titulo, null) ?: return
        var lancador: ActivityResultLauncher<Intent>? = null
        lancador = activityResultRegistry.register(
            "mesasapp_senha_do_celular",
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->
            lancador?.unregister()
            if (resultado.resultCode == Activity.RESULT_OK) aoPassar()
        }
        lancador.launch(intent)
    }
}
