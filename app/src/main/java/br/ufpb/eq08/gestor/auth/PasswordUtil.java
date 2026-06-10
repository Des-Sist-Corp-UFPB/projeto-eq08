package br.ufpb.eq08.gestor.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitário para hash e verificação de senhas com BCrypt.
 */
public final class PasswordUtil {

    // Custo do BCrypt (12 = boa segurança sem ser muito lento)
    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {}

    /**
     * Gera o hash BCrypt de uma senha em texto puro.
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser vazia.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Verifica se uma senha em texto puro corresponde ao hash armazenado.
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
