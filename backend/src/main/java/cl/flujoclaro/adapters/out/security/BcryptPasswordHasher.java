package cl.flujoclaro.adapters.out.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import cl.flujoclaro.domain.port.PasswordHasherPort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasherPort {
    @Override
    public String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), hash).verified;
    }
}
