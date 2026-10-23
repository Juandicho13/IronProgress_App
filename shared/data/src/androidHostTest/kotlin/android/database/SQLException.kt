package android.database

/**
 * Solo para las pruebas de host. En Android, el SQLiteException de androidx.sqlite es esta clase,
 * pero el android.jar de pruebas la trae vacía y pierde el mensaje de SQLite. Room lee ese mensaje
 * en @Upsert para saber si hubo un choque de clave y actualizar en vez de insertar, así que sin
 * esta copia cualquier upsert sobre una fila existente falla en las pruebas (en el teléfono no).
 */
open class SQLException : RuntimeException {
    constructor() : super()
    constructor(error: String?) : super(error)
    constructor(error: String?, cause: Throwable?) : super(error, cause)
}
