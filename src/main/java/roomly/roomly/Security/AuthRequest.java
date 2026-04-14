package roomly.roomly.Security;

// DTO ligero para recibir credenciales en el endpoint de login
public record AuthRequest(String email, String password) {}
