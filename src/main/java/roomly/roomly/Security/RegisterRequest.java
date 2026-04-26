package roomly.roomly.Security;

// DTO para registrar nuevos usuarios.
// Añadimos `company` para recibir el nombre de la compañia que será usado como tenant.
public record RegisterRequest(String password, String email, String company, String firstName, String lastName) {}
