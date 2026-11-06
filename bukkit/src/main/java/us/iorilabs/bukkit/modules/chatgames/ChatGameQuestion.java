package us.iorilabs.bukkit.modules.chatgames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class ChatGameQuestion {

    private final String question;
    private final List<String> answers;

    public ChatGameQuestion(String question, List<String> answers) {
        this.question = question == null ? "" : question;
        List<String> normalized = new ArrayList<>();
        if (answers != null) {
            for (String answer : answers) {
                if (answer == null || answer.trim().isEmpty()) continue;
                normalized.add(answer.trim().toLowerCase(Locale.ROOT));
            }
        }
        this.answers = Collections.unmodifiableList(normalized);
    }

    public boolean matches(String input) {
        if (input == null || answers.isEmpty()) return false;
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return answers.contains(normalized);
    }

    public String primaryAnswer() {
        return answers.isEmpty() ? "" : answers.get(0);
    }
}
