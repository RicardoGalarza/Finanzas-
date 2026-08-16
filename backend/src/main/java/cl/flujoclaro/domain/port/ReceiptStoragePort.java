package cl.flujoclaro.domain.port;

import java.nio.file.Path;

public interface ReceiptStoragePort {

    record StoredReceipt(byte[] content, String contentType, String filename) {}

    String save(Path source, String originalFilename, String contentType);

    StoredReceipt load(String storageKey);

    void delete(String storageKey);
}
