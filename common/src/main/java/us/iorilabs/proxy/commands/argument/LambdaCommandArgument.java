package us.iorilabs.proxy.commands.argument;

import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class LambdaCommandArgument extends CommandArgument {

    private final Executor executor;

    public LambdaCommandArgument(String name, String permission, Executor executor) {
        super(name, permission);
        this.executor = executor;
    }

    public static LambdaCommandArgument of(String name, Executor executor) {
        return new LambdaCommandArgument(name, null, executor);
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        executor.run(source, args);
    }

    @FunctionalInterface
    public interface Executor {
        void run(CommandSource source, String[] args);
    }
}
