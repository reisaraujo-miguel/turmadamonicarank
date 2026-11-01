package rocks.leug.turmadamonicarank.mail;

public record EmailMessage(String to, String subject, String text, String html) {
}
