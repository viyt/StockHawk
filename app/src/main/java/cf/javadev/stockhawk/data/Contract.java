package cf.javadev.stockhawk.data;


import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.collect.ImmutableList;

public final class Contract {
    static final String AUTHORITY = "cf.javadev.stockhawk";
    static final String PATH_QUOTE = "quote";
    static final String PATH_HISTORY = "history";
    static final String PATH_QUOTE_WITH_SYMBOL = "quote/*";
    static final String PATH_HISTORY_WITH_SYMBOL = "history/*";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
    }

    @SuppressWarnings("unused")
    public static final class Quote implements BaseColumns {
        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_QUOTE).build();

        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_COMPANY_NAME = "company_name";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_ABSOLUTE_CHANGE = "absolute_change";
        public static final String COLUMN_PERCENTAGE_CHANGE = "percentage_change";
        public static final String COLUMN_DATE_TIME_UPDATE = "date_time_update";
        public static final String COLUMN_PREV_CLOSE = "prev_close";
        public static final String COLUMN_OPEN = "open";
        public static final String COLUMN_LOW = "low";
        public static final String COLUMN_HIGH = "high";


        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_COMPANY_NAME = 2;
        public static final int POSITION_PRICE = 3;
        public static final int POSITION_ABSOLUTE_CHANGE = 4;
        public static final int POSITION_PERCENTAGE_CHANGE = 5;
        public static final int POSITION_DATE_TIME_UPDATE = 6;
        public static final int POSITION_PREV_CLOSE = 7;
        public static final int POSITION_OPEN = 8;
        public static final int POSITION_LOW = 9;
        public static final int POSITION_HIGH = 10;


        public static final ImmutableList<String> QUOTE_COLUMNS_PROJECTION = ImmutableList.of(
                _ID,
                COLUMN_SYMBOL,
                COLUMN_COMPANY_NAME,
                COLUMN_PRICE,
                COLUMN_ABSOLUTE_CHANGE,
                COLUMN_PERCENTAGE_CHANGE,
                COLUMN_DATE_TIME_UPDATE,
                COLUMN_PREV_CLOSE,
                COLUMN_OPEN,
                COLUMN_LOW,
                COLUMN_HIGH
        );
        static final String TABLE_NAME = "quotes";

        public static Uri makeUriForStock(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }
    }

    public static final class History implements BaseColumns {
        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_HISTORY).build();

        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_HISTORY_ONE_MONTH = "history_one_month";
        public static final String COLUMN_HISTORY_SIX_MONTHS = "history_six_month";
        public static final String COLUMN_HISTORY_MAX_YEARS = "history_max_years";
        public static final String COLUMN_HISTORY_UPDATE = "history_update";

        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_HISTORY_ONE_MONTH = 2;
        public static final int POSITION_HISTORY_SIX_MONTHS = 3;
        public static final int POSITION_HISTORY_MAX_YEARS = 4;
        public static final int POSITION_HISTORY_UPDATE = 5;

        public static final ImmutableList<String> HISTORY_COLUMNS_PROJECTION = ImmutableList.of(
                _ID,
                COLUMN_SYMBOL,
                COLUMN_HISTORY_ONE_MONTH,
                COLUMN_HISTORY_SIX_MONTHS,
                COLUMN_HISTORY_MAX_YEARS,
                COLUMN_HISTORY_UPDATE
        );
        static final String TABLE_NAME = "history";

        public static Uri makeUriForStock(String symbol) {
            System.out.println();
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }
    }
}
