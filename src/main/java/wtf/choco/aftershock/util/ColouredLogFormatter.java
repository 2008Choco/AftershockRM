package wtf.choco.aftershock.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class ColouredLogFormatter extends Formatter {

    // ANSI escape code
    public static final String RESET = "\u001B[0m";

    public static final String BLACK = "\u001B[30m";
    public static final String DARK_RED = "\u001B[31m";
    public static final String DARK_GREEN = "\u001B[32m";
    public static final String DARK_YELLOW = "\u001B[33m";
    public static final String DARK_BLUE = "\u001B[34m";
    public static final String DARK_PURPLE = "\u001B[35m";
    public static final String DARK_CYAN = "\u001B[36m";
    public static final String DARK_GRAY = "\u001B[90m";

    public static final String GRAY = "\u001B[37m";
    public static final String RED = "\u001B[91m";
    public static final String GREEN = "\u001B[92m";
    public static final String YELLOW = "\u001B[93m";
    public static final String BLUE = "\u001B[94m";
    public static final String MAGENTA = "\u001B[95m";
    public static final String CYAN = "\u001B[96m";
    public static final String WHITE = "\u001B[97m";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ColouredLogFormatter INSTANCE = new ColouredLogFormatter();

    private ColouredLogFormatter() { }

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append(DARK_CYAN);

        builder.append("[");
        builder.append(DATE_FORMAT.format(new Date(record.getMillis())));
        builder.append("]");

        Level level = record.getLevel();
        if (level == Level.INFO) {
            builder.append(DARK_GRAY);
        } else if (level == Level.WARNING) {
            builder.append(DARK_YELLOW);
        } else if (level == Level.SEVERE) {
            builder.append(RED);
        }

        builder.append(" [");
        builder.append(record.getLevel().getLocalizedName());
        builder.append("] ");

        builder.append(BLACK);
        builder.append(record.getMessage());

        Object[] params = record.getParameters();

        if (params != null) {
            builder.append("\t");

            for (int i = 0; i < params.length; i++) {
                builder.append(params[i]);
                if (i < params.length - 1) {
                    builder.append(", ");
                }
            }
        }

        return builder.append(RESET).append("\n").toString();
    }

    public static ColouredLogFormatter get() {
        return INSTANCE;
    }

}
