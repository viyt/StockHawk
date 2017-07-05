package cf.javadev.stockhawk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


class DbHelper extends SQLiteOpenHelper {
    private static final String NAME = "StockHawk.db";
    private static final int VERSION = 1;

    DbHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String builderQuote = "CREATE TABLE " + Contract.Quote.TABLE_NAME + " ("
                + Contract.Quote._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Contract.Quote.COLUMN_SYMBOL + " TEXT NOT NULL, "
                + Contract.Quote.COLUMN_COMPANY_NAME + " TEXT NOT NULL, "
                + Contract.Quote.COLUMN_PRICE + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_ABSOLUTE_CHANGE + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_PERCENTAGE_CHANGE + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_DATE_TIME_UPDATE + " INTEGER NOT NULL, "
                + Contract.Quote.COLUMN_PREV_CLOSE + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_OPEN + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_LOW + " REAL NOT NULL, "
                + Contract.Quote.COLUMN_HIGH + " REAL NOT NULL, "
                + "UNIQUE (" + Contract.Quote.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";

        String builderHistory = "CREATE TABLE " + Contract.History.TABLE_NAME + " ("
                + Contract.History._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Contract.History.COLUMN_SYMBOL + " TEXT NOT NULL, "
                + Contract.History.COLUMN_HISTORY_ONE_MONTH + " TEXT NOT NULL, "
                + Contract.History.COLUMN_HISTORY_SIX_MONTHS + " TEXT NOT NULL, "
                + Contract.History.COLUMN_HISTORY_MAX_YEARS + " TEXT NOT NULL, "
                + Contract.History.COLUMN_HISTORY_UPDATE + " TEXT NOT NULL, "
                + "UNIQUE (" + Contract.Quote.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";

        db.execSQL(builderQuote);
        db.execSQL(builderHistory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(" DROP TABLE IF EXISTS " + Contract.Quote.TABLE_NAME);
        db.execSQL(" DROP TABLE IF EXISTS " + Contract.History.TABLE_NAME);
        onCreate(db);
    }
}
