package us.iorilabs.bukkit.modules.chatgames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.modules.chatgames.enums.ChatGameType;

@Getter
@Accessors(fluent = true)
public final class ChatGameRound {

    private final ChatGameType type;
    private final String question;
    private final List<String> answers;

    public ChatGameRound(ChatGameType type, String question, List<String> answers) {
        this.type = type == null ? ChatGameType.TRIVIA : type;
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
        return answers.contains(input.trim().toLowerCase(Locale.ROOT));
    }

    public String primaryAnswer() {
        return answers.isEmpty() ? "" : answers.get(0);
    }
}
