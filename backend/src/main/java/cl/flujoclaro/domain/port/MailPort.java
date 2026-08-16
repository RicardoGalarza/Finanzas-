package cl.flujoclaro.domain.port;

public interface MailPort {
    void send(String to, String subject, String textBody);

    default void send(String to, String subject, String textBody, String htmlBody) {
        send(to, subject, textBody);
    }
}
