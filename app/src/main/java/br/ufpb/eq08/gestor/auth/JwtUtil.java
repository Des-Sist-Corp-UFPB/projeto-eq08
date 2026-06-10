package br.ufpb.eq08.gestor.auth;

import br.ufpb.eq08.gestor.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Utilitário para geração e validação de tokens JWT.
 * Usa HMAC-SHA256 com chave configurada via variável de ambiente.
 */
public final class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final SecretKey SECRET_KEY = buildKey();

    private JwtUtil() {}

    private static SecretKey buildKey() {
        byte[] keyBytes = AppConfig.JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        // Garante ao menos 32 bytes (256 bits) para HMAC-SHA256
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um JWT de acesso para o usuário.
     *
     * @param userId   UUID do usuário
     * @param tenantId UUID do tenant (pode ser null para SUPER_ADMIN)
     * @param role     Papel do usuário
     */
    public static String generateToken(UUID userId, UUID tenantId, String role) {
        long expiryMs = AppConfig.JWT_EXPIRY_DAYS * 24L * 60 * 60 * 1000;
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId != null ? tenantId.toString() : null)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Valida e retorna as claims do token.
     *
     * @param token JWT a ser validado
     * @return Claims do token, ou null se inválido/expirado
     */
    public static Claims validateToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o UUID do usuário de um token válido.
     */
    public static UUID extractUserId(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extrai o tenantId de um token válido.
     */
    public static UUID extractTenantId(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        String tenantId = claims.get("tenantId", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    /**
     * Extrai o role de um token válido.
     */
    public static String extractRole(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        return claims.get("role", String.class);
    }
}
