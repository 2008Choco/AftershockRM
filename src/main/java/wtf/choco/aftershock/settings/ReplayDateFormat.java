package wtf.choco.aftershock.settings;

import wtf.choco.aftershock.util.Translatable;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.function.UnaryOperator;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public enum ReplayDateFormat implements Translatable {

    DAY_MONTH_YEAR_NUMERIC(f -> f
            .appendValue(DAY_OF_MONTH, 2).appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2).appendLiteral('-')
            .appendValue(YEAR, 4)
    ),
    MONTH_DAY_YEAR_NUMERIC(f -> f
            .appendValue(MONTH_OF_YEAR, 2).appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2).appendLiteral('-')
            .appendValue(YEAR, 4)
    ),
    DAY_MONTH_YEAR_SHORT(f -> f
            .appendValue(DAY_OF_MONTH, 2).appendLiteral(' ')
            .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(", ")
            .appendValue(YEAR, 4)
    ),
    MONTH_DAY_YEAR_SHORT(f -> f
            .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
            .appendValue(DAY_OF_MONTH, 2).appendLiteral(", ")
            .appendValue(YEAR, 4)
    ),
    DAY_MONTH_YEAR_LONG(f -> f
            .appendText(MONTH_OF_YEAR, TextStyle.FULL).appendLiteral(' ')
            .appendValue(DAY_OF_MONTH, 2).appendLiteral(", ")
            .appendValue(YEAR, 4)
    ),
    MONTH_DAY_YEAR_LONG(f -> f
            .appendValue(DAY_OF_MONTH, 2).appendLiteral(' ')
            .appendText(MONTH_OF_YEAR, TextStyle.FULL).appendLiteral(", ")
            .appendValue(YEAR, 4)
    );

    private static final String RESOURCE_KEY_PREFIX = "ui.settings.value.date_format";

    private String resourceKey;

    private final DateTimeFormatter dateFormatter;

    private ReplayDateFormat(UnaryOperator<DateTimeFormatterBuilder> formatterFunction) {
        this.dateFormatter = formatterFunction.apply(new DateTimeFormatterBuilder()).toFormatter();
    }

    @Override
    public String getResourceKey() {
        if (resourceKey == null) {
            this.resourceKey = RESOURCE_KEY_PREFIX + "." + name().toLowerCase();
        }

        return resourceKey;
    }

    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

}
