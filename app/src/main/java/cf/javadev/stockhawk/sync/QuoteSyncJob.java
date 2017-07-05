package cf.javadev.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.javadev.stockhawk.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cf.javadev.stockhawk.data.Contract;
import cf.javadev.stockhawk.data.PrefUtils;
import cf.javadev.stockhawk.utils.ConnectivityUtil;
import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {
    private static final int ONE_OFF_ID = 2;
    private static final String ACTION_DATA_UPDATED = "cf.javadev.stockhawk.ACTION_DATA_UPDATED";
    private static final String ERROR_MESSAGE = "cf.javadev.stockhawk.ERROR_MESSAGE";
    private static final String MESSAGE_TEXT = "message_text";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int MONTH_ONE_HISTORY = 1;
    private static final int MONTH_SIX_HISTORY = 6;
    private static final int YEARS_MAX_HISTORY = 80;
    private static final String LOG_TAG = "my";

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {
        Timber.d("Running sync job");

        Calendar fromOneMonth = Calendar.getInstance();
        Calendar fromSixMonths = Calendar.getInstance();
        Calendar fromMaxYears = Calendar.getInstance();

        Calendar to = Calendar.getInstance();

        fromOneMonth.add(Calendar.MONTH, -MONTH_ONE_HISTORY);
        fromSixMonths.add(Calendar.MONTH, -MONTH_SIX_HISTORY);
        fromMaxYears.add(Calendar.YEAR, -YEARS_MAX_HISTORY);

        try {
            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();
            ArrayList<ContentValues> historyCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();

                Stock stock;
                String name;
                float price;
                float change;
                float percentChange;
                long dateUpdate;
                float prevClose;
                float open;
                float low;
                float high;

                try {
                    stock = quotes.get(symbol);
                    StockQuote quote = stock.getQuote();
                    name = stock.getName();
                    price = quote.getPrice().floatValue();
                    change = quote.getChange().floatValue();
                    percentChange = quote.getChangeInPercent().floatValue();
                    dateUpdate = new Date().getTime();
                    prevClose = quote.getPreviousClose().floatValue();
                    open = quote.getOpen().floatValue();
                    low = quote.getDayLow().floatValue();
                    high = quote.getDayHigh().floatValue();

                } catch (NullPointerException e) {
                    String message = context.getString(R.string.message_symbol_not_found);
                    Timber.e(e, message);
                    PrefUtils.removeStock(context, symbol);
                    sendBroadcastError(context, message);
                    continue;
                }

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_COMPANY_NAME, name);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                quoteCV.put(Contract.Quote.COLUMN_DATE_TIME_UPDATE, dateUpdate);
                quoteCV.put(Contract.Quote.COLUMN_PREV_CLOSE, prevClose);
                quoteCV.put(Contract.Quote.COLUMN_OPEN, open);
                quoteCV.put(Contract.Quote.COLUMN_LOW, low);
                quoteCV.put(Contract.Quote.COLUMN_HIGH, high);
                quoteCVs.add(quoteCV);

                ContentValues historyCV;
                if (isNeedToUpdate(context, symbol)) {
                    historyCV = new ContentValues();

                    List<HistoricalQuote> historyOneMonth =
                            stock.getHistory(fromOneMonth, to, Interval.DAILY);
                    List<HistoricalQuote> historySixMonth =
                            stock.getHistory(fromSixMonths, to, Interval.WEEKLY);
                    List<HistoricalQuote> historyMaxYears =
                            stock.getHistory(fromMaxYears, to, Interval.MONTHLY);
                    historyCV.put(Contract.History.COLUMN_SYMBOL, symbol);
                    historyCV.put(Contract.History.COLUMN_HISTORY_ONE_MONTH,
                            getHistoryDataString(historyOneMonth));
                    historyCV.put(Contract.History.COLUMN_HISTORY_SIX_MONTHS,
                            getHistoryDataString(historySixMonth));
                    historyCV.put(Contract.History.COLUMN_HISTORY_MAX_YEARS,
                            getHistoryDataString(historyMaxYears));
                    historyCV.put(Contract.History.COLUMN_HISTORY_UPDATE, getCurrentDate());
                    historyCVs.add(historyCV);
                }
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));
            if (!historyCVs.isEmpty()) {
                context.getContentResolver()
                        .bulkInsert(
                                Contract.History.URI,
                                historyCVs.toArray(new ContentValues[historyCVs.size()]));
            }

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);
            Log.d(LOG_TAG, "QuoteSyncJob,  getQuotes: send broadcast update");
        } catch (IOException exception) {
            String message = context.getString(R.string.message_error_fetching_stock);
            Timber.e(exception, message);
            sendBroadcastError(context, message);
        }
    }

    private static void sendBroadcastError(final Context context, String message) {
        Intent intent = new Intent(ERROR_MESSAGE);
        intent.putExtra(MESSAGE_TEXT, message);
        context.sendBroadcast(intent);
    }

    private static boolean isNeedToUpdate(final Context context, String symbol) {
        String lastUpdateHistory = null;
        Cursor cursor = null;
        String selection = Contract.History.COLUMN_SYMBOL + "=?";
        String[] selectionArgs = {symbol};
        try {
            cursor = context.getContentResolver().query(
                    Contract.History.URI,
                    Contract.History.HISTORY_COLUMNS_PROJECTION.toArray(new String[]{}),
                    selection,
                    selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                lastUpdateHistory = cursor.getString(Contract.History.POSITION_HISTORY_UPDATE);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String currentDate = getCurrentDate();
        return !currentDate.equals(lastUpdateHistory);
    }

    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    @NonNull
    private static String getHistoryDataString(List<HistoricalQuote> quotes) {
        StringBuilder historyBuilder = new StringBuilder();
        for (HistoricalQuote it : quotes) {
            historyBuilder.append(it.getDate().getTimeInMillis());
            historyBuilder.append(", ");
            historyBuilder.append(it.getClose());
            historyBuilder.append("\n");
        }
        return historyBuilder.toString();
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID,
                new ComponentName(context, QuoteJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        JobScheduler scheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    public static synchronized void initialize(final Context context) {
        schedulePeriodic(context);
        syncImmediately(context);
    }

    public static synchronized void syncImmediately(Context context) {
        if (ConnectivityUtil.networkUp(context)) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID,
                    new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
            JobScheduler scheduler = (JobScheduler)
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }
}
