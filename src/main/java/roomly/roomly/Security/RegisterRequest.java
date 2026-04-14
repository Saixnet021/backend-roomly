package roomly.roomly.Security;

// DTO para registrar nuevos usuarios (username, password, email opcional)
public record RegisterRequest(String username, String password, String email) {}
