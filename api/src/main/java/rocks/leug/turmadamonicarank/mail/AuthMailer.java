package rocks.leug.turmadamonicarank.mail;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import rocks.leug.turmadamonicarank.config.WebProperties;
import rocks.leug.turmadamonicarank.user.User;

@Component
public class AuthMailer {

    private final EmailSender emailSender;
    private final WebProperties webProperties;

    public AuthMailer(EmailSender emailSender, WebProperties webProperties) {
        this.emailSender = emailSender;
        this.webProperties = webProperties;
    }

    public void sendConfirmation(User user, String token) {
        String link = webProperties.linkBase() + "/confirmar-email?token=" + token;
        String name = greetingName(user);
        String text = """
                Olá, %s!

                Para confirmar seu e-mail na Turma da Mônica Rank, abra este link:
                %s

                O link vale por 24 horas e só pode ser usado uma vez.
                Se você não criou esta conta, ignore esta mensagem.
                """.formatted(name, link);
        String html = paragraph(name, link, "Confirmar e-mail");
        emailSender.send(new EmailMessage(
                user.getEmail(), "Confirme seu e-mail — Turma da Mônica Rank", text, html));
    }

    public void sendPasswordReset(User user, String token) {
        String link = webProperties.linkBase() + "/redefinir-senha?token=" + token;
        String name = greetingName(user);
        String text = """
                Olá, %s!

                Recebemos um pedido para redefinir a senha da sua conta na
                Turma da Mônica Rank. Abra este link para escolher uma nova senha:
                %s

                O link vale por 1 hora e só pode ser usado uma vez.
                Se não foi você, ignore esta mensagem; sua senha continua a mesma.
                """.formatted(name, link);
        String html = paragraph(name, link, "Criar nova senha");
        emailSender.send(new EmailMessage(
                user.getEmail(), "Redefina sua senha — Turma da Mônica Rank", text, html));
    }

    private static String greetingName(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? "fã"
                : user.getDisplayName();
    }

    private static String paragraph(String name, String link, String action) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <body style="margin:0;background:#FFFDF6;color:#14110E;font-family:system-ui,sans-serif">
                    <div style="max-width:520px;margin:32px auto;padding:24px;border:3px solid #14110E;border-radius:10px;background:#FFFDF6">
                      <p>Olá, %s!</p>
                      <p>Use o botão abaixo para continuar. O link é pessoal e expira em breve.</p>
                      <p>
                        <a href="%s" style="display:inline-block;padding:12px 20px;border:2px solid #14110E;border-radius:8px;background:#C92F45;color:#FFFDF6;text-decoration:none;font-weight:700">%s</a>
                      </p>
                      <p style="font-size:13px;color:#14110E99">Se você não pediu isso, ignore esta mensagem.</p>
                    </div>
                  </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(name),
                HtmlUtils.htmlEscape(link),
                HtmlUtils.htmlEscape(action));
    }
}
