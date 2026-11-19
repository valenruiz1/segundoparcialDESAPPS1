## 41 - Programa sin señal

Antes, si fallaba la descarga, aparecía el error y dejaba de verse la lista, aunque las charlas siguieran guardadas en Room.

- En `Programa.kt` agregué `EstadoPrograma` para guardar si alguna vez se descargó bien el programa. Esto sirve porque una lista vacía también puede ser una respuesta válida.
- Pasé Room de la versión 1 a la 2 y agregué la migración en `Servicios.kt`, para no borrar los datos que ya tenía alguien.
- Cambié `AgendaRepository.kt` para guardar el programa completo cuando la descarga sale bien, incluso si viene vacío.
- En `ProgramaModel.kt`, `Catalogo.kt` y `Portada.kt` cambié los mensajes y la forma de mostrar el error. Si hay datos guardados, se siguen viendo y aparece el aviso. Si nunca se descargó nada, se puede reintentar.

## 42 - Favoritas

El problema era que al actualizar se perdían las charlas marcadas y algunas charlas viejas podían quedar en la lista.

En `Programa.kt` cambié `reemplazarInstantanea()`. Ahora, cuando llega la descarga, consulta las favoritas que hay en Room y las conserva según el ID de cada charla. Si una charla cambió de nombre o sala, sigue marcada. Si desaparece del programa, se borra. Y si vuelve después, vuelve sin marcar.

Esto se hace dentro de una transacción para guardar todo junto. También sirve para no perder una favorita que se marcó mientras se estaba descargando.

## 43 - Recuperar los filtros

Acá el problema era que al salir de la lista o cuando Android cerraba el proceso se podían perder los filtros.

- En `ProgramaModel.kt` usé `SavedStateHandle` para guardar el día y el tema elegidos.
- En `Catalogo.kt` saqué el `remember` del tema y ahora leo los filtros desde el ViewModel con `StateFlow`.
- La ficha abierta ya usaba `rememberSaveable` en `Portada.kt`, así que no cambié eso.

## 44 - Descargas repetidas

Había descargas automáticas en `onStart()` y también en `LaunchedEffect` del catálogo. Entonces podía descargar otra vez al girar el celular o al volver a una pantalla.

- En `MainActivity.kt` saqué la descarga de `onStart()` y dejé una carga inicial controlada.
- En `Catalogo.kt` saqué el `LaunchedEffect` que descargaba.
- En `ProgramaModel.kt` agregué el control para que haya una sola carga automática por proceso. Si se toca Actualizar mientras ya está descargando, deja como máximo una actualización pendiente, en vez de lanzar varias juntas.
- En `ServicioLocal.kt` hice que el contador se comparta durante el proceso.
