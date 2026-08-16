package cl.flujoclaro.domain.port;

import java.nio.file.Path;

public interface AvatarStoragePort {

    record StoredAvatar(byte[] content, String contentType) {}

    String save(Path source, String contentType);

    StoredAvatar load(String storageKey);

    void delete(String storageKey);
}
