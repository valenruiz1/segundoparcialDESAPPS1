package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val descargaExitosa = repo.descargaExitosa.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dia = savedStateHandle.getStateFlow("dia", 0)
    val tema = savedStateHandle.getStateFlow("tema", "Todos")

    fun cambiarDia(valor: Int) { savedStateHandle["dia"] = valor }
    fun cambiarTema(valor: String) { savedStateHandle["tema"] = valor }
    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    private var actualizacionPendiente = false

    fun iniciarCargaAutomatica() {
        val corresponde = synchronized(bloqueo) {
            if (cargaAutomaticaIniciada) false
            else {
                cargaAutomaticaIniciada = true
                true
            }
        }
        if (corresponde) iniciarDescarga()
    }

    fun recargar() {
        if (cargando.value) {
            actualizacionPendiente = true
            return
        }
        iniciarDescarga()
    }

    private fun iniciarDescarga() {
        cargando.value = true
        viewModelScope.launch {
            try {
                do {
                    actualizacionPendiente = false
                    aviso.value = ""
                    try {
                        repo.sincronizar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val hayCopiaLocal = repo.descargaExitosa.first()
                        aviso.value = if (hayCopiaLocal) {
                            "No se pudo actualizar el programa. Se están usando los datos guardados."
                        } else {
                            "No se pudo consultar el programa. Comprobá la conexión e intentá nuevamente."
                        }
                    }
                } while (actualizacionPendiente)
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }

    companion object {
        private val bloqueo = Any()
        private var cargaAutomaticaIniciada = false
    }
}
