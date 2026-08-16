package cl.flujoclaro.adapters.out.storage;

import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.port.AvatarStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LocalAvatarStorageAdapter implements AvatarStoragePort {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path directory;

    public LocalAvatarStorageAdapter(
            @ConfigProperty(name = "app.upload.dir", defaultValue = "./uploads") String uploadDirectory) {
        this.directory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("avatars");
    }

    @Override
    public String save(Path source, String contentType) {
        String extension = TYPES.get(contentType);
        if (extension == null) {
            throw new DomainException("La foto debe ser JPG, PNG o WEBP");
        }
        try {
            long size = Files.size(source);
            if (size == 0 || size > MAX_SIZE) {
                throw new DomainException("La foto debe pesar entre 1 byte y 5 MB");
            }
            Files.createDirectories(directory);
            String key = UUID.randomUUID() + extension;
            Files.copy(source, resolve(key), StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (DomainException e) {
            throw e;
        } catch (IOException e) {
            throw new DomainException("No se pudo guardar la foto de perfil");
        }
    }

    @Override
    public StoredAvatar load(String storageKey) {
        Path file = resolve(storageKey);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Foto de perfil no encontrada");
        }
        try {
            return new StoredAvatar(Files.readAllBytes(file), contentType(storageKey));
        } catch (IOException e) {
            throw new DomainException("No se pudo leer la foto de perfil");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new DomainException("No se pudo eliminar la foto anterior");
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new NotFoundException("Foto de perfil no encontrada");
        }
        Path path = directory.resolve(key).normalize();
        if (!path.getParent().equals(directory)) {
            throw new DomainException("Ruta de foto inválida");
        }
        return path;
    }

    private String contentType(String key) {
        String extension = key.substring(key.lastIndexOf('.')).toLowerCase();
        return TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(extension))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }
}
