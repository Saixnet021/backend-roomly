package roomly.roomly.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que extrae el primer segmento de la ruta como tenant.
 * Ejemplo: /casanovedades/api/usuarios -> tenantRaw = "casanovedades"
 * Normalizamos (quitamos espacios, lower-case) y agregamos '/' final según la petición del usuario.
 */
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI(); // /{tenant}/...  o /api/... si no tiene tenant
        String[] parts = path.split("/");
        if (parts.length > 1 && parts[1] != null && !parts[1].isBlank()) {
            // parts[1] es el primer segmento útil
            String tenantRaw = parts[1];
            // Normalizamos: eliminamos espacios y ponemos en minúsculas
            String normalized = tenantRaw.replaceAll("\\s+", "").toLowerCase();
            // Guardamos el slug normalizado (ej: "casanovedades") sin barras.
            TenantContext.set(normalized);
        } else {
            // No hay tenant en la ruta; dejamos el TenantContext en null.
            // Permitimos esto para endpoints de super-admin (login del dueño del SaaS).
            TenantContext.set(null);
        }
        return true; // siempre permitimos continuar; validaciones específicas se hacen en controladores
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
        // limpiamos el contexto al finalizar la petición
        TenantContext.clear();
    }
}
