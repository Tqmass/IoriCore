package us.iorilabs.bukkit.modules.chatgames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import us.iorilabs.bukkit.modules.chatgames.enums.ChatGameType;

final class ChatGameGenerators {

    private ChatGameGenerators() {}

    static ChatGameRound trivia(List<ChatGameQuestion> questions) {
        if (questions == null || questions.isEmpty()) return null;
        ChatGameQuestion picked = questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
        return new ChatGameRound(ChatGameType.TRIVIA, picked.question(), new ArrayList<>(picked.answers()));
    }

    static ChatGameRound reaction(List<String> words) {
        String word = pickWord(words);
        if (word == null) return null;
        return new ChatGameRound(ChatGameType.REACTION, word, List.of(word));
    }

    static ChatGameRound unscramble(List<String> words) {
        String word = pickWord(words);
        if (word == null) return null;
        return new ChatGameRound(ChatGameType.UNSCRAMBLE, scramble(word), List.of(word));
    }

    static ChatGameRound math(int min, int max, boolean hard) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        if (hard) {
            int a = random.nextInt(lo, hi + 1);
            int b = random.nextInt(lo, hi + 1);
            int c = random.nextInt(lo, hi + 1);
            int d = random.nextInt(lo, hi + 1);
            char op1 = ops()[random.nextInt(ops().length)];
            char op2 = ops()[random.nextInt(ops().length)];
            char op3 = ops()[random.nextInt(ops().length)];
            String equation = "(" + a + " " + op1 + " " + b + ") " + op2 + " (" + c + " " + op3 + " " + d + ")";
            int left = apply(a, b, op1);
            int right = apply(c, d, op3);
            if (left == Integer.MIN_VALUE || right == Integer.MIN_VALUE) return math(min, max, false);
            int answer = apply(left, right, op2);
            if (answer == Integer.MIN_VALUE) return math(min, max, false);
            return new ChatGameRound(ChatGameType.MATH, equation, List.of(String.valueOf(answer)));
        }
        int a = random.nextInt(lo, hi + 1);
        int b = random.nextInt(lo, hi + 1);
        char op = ops()[random.nextInt(ops().length)];
        int answer = apply(a, b, op);
        if (answer == Integer.MIN_VALUE) {
            op = '+';
            answer = a + b;
        }
        return new ChatGameRound(ChatGameType.MATH, a + " " + op + " " + b, List.of(String.valueOf(answer)));
    }

    static ChatGameRound algebra(int min, int max) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        int x = random.nextInt(lo, hi + 1);
        int a = random.nextInt(2, 6);
        int b = random.nextInt(0, hi + 1);
        boolean add = random.nextBoolean();
        int c = add ? a * x + b : a * x - b;
        String equation = add ? a + "x + " + b + " = " + c : a + "x - " + b + " = " + c;
        return new ChatGameRound(ChatGameType.ALGEBRA, equation, List.of(String.valueOf(x)));
    }

    private static String pickWord(List<String> words) {
        if (words == null || words.isEmpty()) return null;
        String word = words.get(ThreadLocalRandom.current().nextInt(words.size()));
        if (word == null || word.trim().isEmpty()) return null;
        return word.trim().toLowerCase(Locale.ROOT);
    }

    private static String scramble(String word) {
        List<Character> chars = new ArrayList<>(word.length());
        for (char c : word.toCharArray()) chars.add(c);
        for (int attempt = 0; attempt < 8; attempt++) {
            Collections.shuffle(chars, ThreadLocalRandom.current());
            StringBuilder builder = new StringBuilder(chars.size());
            for (Character c : chars) builder.append(c);
            String scrambled = builder.toString();
            if (!scrambled.equals(word) || word.length() <= 1) return scrambled;
        }
        return word;
    }

    private static char[] ops() {
        return new char[] { '+', '-', '*', '/' };
    }

    private static int apply(int left, int right, char op) {
        switch (op) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                if (right == 0 || left % right != 0) return Integer.MIN_VALUE;
                return left / right;
            default:
                return Integer.MIN_VALUE;
        }
    }
}
