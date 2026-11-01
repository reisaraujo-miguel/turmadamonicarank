package rocks.leug.turmadamonicarank;

import java.util.ArrayList;
import java.util.List;

import rocks.leug.turmadamonicarank.mail.EmailMessage;
import rocks.leug.turmadamonicarank.mail.EmailSender;

public class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> messages = new ArrayList<>();

    @Override
    public void send(EmailMessage message) {
        messages.add(message);
    }

    public List<EmailMessage> messages() {
        return List.copyOf(messages);
    }

    public EmailMessage lastMessage() {
        if (messages.isEmpty()) {
            throw new AssertionError("No email was sent");
        }
        return messages.get(messages.size() - 1);
    }

    public void clear() {
        messages.clear();
    }
}
