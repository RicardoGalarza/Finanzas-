package cl.flujoclaro.adapters.out.storage;

import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.port.ReceiptStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LocalReceiptStorageAdapter implements ReceiptStoragePort {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );

    private final Path storageDirectory;

    public LocalReceiptStorageAdapter(
            @ConfigProperty(name = "app.upload.dir", defaultValue = "./uploads") String uploadDirectory) {
        this.storageDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String save(Path source, String originalFilename, String contentType) {
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new DomainException("El comprobante debe ser JPG, PNG, WEBP o PDF");
        }
        try {
            long size = Files.size(source);
            if (size == 0 || size > MAX_SIZE) {
                throw new DomainException("El comprobante debe pesar entre 1 byte y 10 MB");
            }
            Files.createDirectories(storageDirectory);
            String storageKey = UUID.randomUUID() + extension;
            Files.copy(source, resolveSafely(storageKey), StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (DomainException e) {
            throw e;
        } catch (IOException e) {
            throw new DomainException("No se pudo guardar el comprobante");
        }
    }

    @Override
    public StoredReceipt load(String storageKey) {
        Path file = resolveSafely(storageKey);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Comprobante no encontrado");
        }
        try {
            return new StoredReceipt(
                    Files.readAllBytes(file),
                    contentTypeFor(storageKey),
                    "comprobante" + extensionOf(storageKey)
            );
        } catch (IOException e) {
            throw new DomainException("No se pudo leer el comprobante");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new DomainException("No se pudo eliminar el comprobante");
        }
    }

    private Path resolveSafely(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new NotFoundException("Comprobante no encontrado");
        }
        Path resolved = storageDirectory.resolve(storageKey).normalize();
        if (!resolved.getParent().equals(storageDirectory)) {
            throw new DomainException("Ruta de comprobante inválida");
        }
        return resolved;
    }

    private String contentTypeFor(String storageKey) {
        String extension = extensionOf(storageKey);
        return ALLOWED_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(extension))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }

    private String extensionOf(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        return dot >= 0 ? storageKey.substring(dot).toLowerCase() : "";
    }
}
