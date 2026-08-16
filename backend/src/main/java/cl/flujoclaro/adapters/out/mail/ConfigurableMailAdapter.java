package cl.flujoclaro.adapters.out.mail;

import cl.flujoclaro.domain.port.MailPort;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ConfigurableMailAdapter implements MailPort {

    private static final Logger LOG = Logger.getLogger(ConfigurableMailAdapter.class);

    private final Mailer mailer;
    private final String mode;

    public ConfigurableMailAdapter(
            Mailer mailer,
            @ConfigProperty(name = "app.mail.mode", defaultValue = "log") String mode) {
        this.mailer = mailer;
        this.mode = mode;
    }

    @Override
    public void send(String to, String subject, String textBody) {
        send(to, subject, textBody, null);
    }

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        if ("smtp".equalsIgnoreCase(mode)) {
            Mail mail = htmlBody == null || htmlBody.isBlank()
                    ? Mail.withText(to, subject, textBody)
                    : Mail.withHtml(to, subject, htmlBody).setText(textBody);
            mailer.send(mail);
            LOG.infof("Correo SMTP enviado a=%s subject=%s", to, subject);
            return;
        }
        LOG.infof("[log] Correo a=%s subject=%s body=%s", to, subject, textBody);
    }
}
