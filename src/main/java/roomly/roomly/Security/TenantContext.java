package roomly.roomly.Security;

/**
 * Contexto simple por petición para almacenar el tenant actual.
 * Usamos ThreadLocal porque cada petición HTTP se maneja en un hilo distinto.
 */
public class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    // Establece el tenant actual (ej: "casanovedades/")
    public static void set(String t) { CURRENT.set(t); }

    // Obtiene el tenant actual para esta petición
    public static String get() { return CURRENT.get(); }

    // Limpia el valor al terminar la petición
    public static void clear() { CURRENT.remove(); }
}
