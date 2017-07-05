package cf.javadev.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.Space;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.javadev.stockhawk.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import cf.javadev.stockhawk.data.Contract;
import cf.javadev.stockhawk.utils.ConnectivityUtil;
import cf.javadev.stockhawk.utils.EntryUtils;

public class StockDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String SAVED_KEY_SYMBOL = "SAVED_SYMBOL";
    private static final String KEY_COLUMN_INDEX = "COLUMN_INDEX";
    private static final String KEY_PERIOD = "PERIOD";
    private static final String DAY_DATA_FORMATTER = "MM.dd.yyyy";
    private static final String MONTH_DATA_FORMATTER = "MMM yyyy";
    private static final String YEAR_DATA_FORMATTER = "yyyy";
    private static final String DATA_TIME_FORMATTER = "dd MMM yyyy hh:mm:ss z";
    private static final int PERIOD_ONE_YEAR = 1;
    private static final int PERIOD_FIVE_YEARS = 5;
    private static final int STOCK_LOADER = 0;
    private static final int HISTORY_LOADER = 1;
    private DecimalFormat dollarFormatWithPlus;
    private DecimalFormat priceFormat;
    private DecimalFormat percentageFormat;

    @BindView(R.id.chart)
    LineChart lineChart;
    @BindView(R.id.period_group)
    RadioGroup radioGroup;
    @BindView(R.id.root_detail)
    LinearLayout rootLayout;
    @BindView(R.id.space_view)
    Space spaceView;
    @BindColor(R.color.material_green_a200)
    int colorGreenA200;
    @BindColor(R.color.material_pink_a100)
    int colorPinkA100;
    @BindColor(android.R.color.white)
    int colorWhite;
    @BindColor(R.color.colorPrimary)
    int colorPrimary;
    private ActionBar actionBar;
    private TextView tvSymbol;
    private TextView tvPrice;
    private TextView tvCurrentPrice;
    private TextView tvLastUpdate;
    private TextView tvQuotePrevClose;
    private TextView tvQuoteOpen;
    private TextView tvQuoteLow;
    private TextView tvQuoteHigh;
    private String symbol;
    private Snackbar snackbar;
    private boolean isLandNotW820;

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ConnectivityUtil.networkUp(getApplicationContext())){
                spaceView.setVisibility(View.VISIBLE);
                showSnackBarWithButtonSetting();
            } else {
                if (snackbar != null) {
                    snackbar.dismiss();
                }
                spaceView.setVisibility(View.GONE);
            }
        }
    };

    private void showSnackBarWithButtonSetting() {
        if (snackbar != null){
            snackbar.dismiss();
        }
        String message = getString(R.string.message_no_connect);
        snackbar = Snackbar.make(rootLayout, message,
                BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(colorPrimary);
        snackbar.setAction(R.string.button_setting, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        ButterKnife.bind(this);
        initActionBar();
        if (savedInstanceState == null) {
            symbol = getIntent().getStringExtra(MainActivity.KEY_SYMBOL);
        } else {
            symbol = savedInstanceState.getString(SAVED_KEY_SYMBOL);
        }

        isLandNotW820 = findViewById(R.id.symbol)!= null;
        initTextField();
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
        initFormats();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            final Bundle bundle = new Bundle();

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_1m:
                        bundle.clear();
                        bundle.putInt(KEY_COLUMN_INDEX, Contract.History.POSITION_HISTORY_ONE_MONTH);
                        initLoaderCallback(bundle);
                        break;
                    case R.id.radio_6m:
                        bundle.clear();
                        bundle.putInt(KEY_COLUMN_INDEX, Contract.History.POSITION_HISTORY_SIX_MONTHS);
                        initLoaderCallback(bundle);
                        break;
                    case R.id.radio_1y:
                        bundle.clear();
                        bundle.putInt(KEY_COLUMN_INDEX, Contract.History.POSITION_HISTORY_MAX_YEARS);
                        bundle.putInt(KEY_PERIOD, PERIOD_ONE_YEAR);
                        initLoaderCallback(bundle);
                        break;
                    case R.id.radio_5y:
                        bundle.clear();
                        bundle.putInt(KEY_COLUMN_INDEX, Contract.History.POSITION_HISTORY_MAX_YEARS);
                        bundle.putInt(KEY_PERIOD, PERIOD_FIVE_YEARS);
                        initLoaderCallback(bundle);
                        break;
                    case R.id.radio_max:
                        bundle.clear();
                        bundle.putInt(KEY_COLUMN_INDEX, Contract.History.POSITION_HISTORY_MAX_YEARS);
                        initLoaderCallback(bundle);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void initTextField() {
        if (isLandNotW820) {
            tvSymbol = (TextView) findViewById(R.id.symbol);
            tvPrice = (TextView)findViewById(R.id.price);
            tvCurrentPrice = (TextView)findViewById(R.id.current_price);
            tvLastUpdate = (TextView) findViewById(R.id.date_last_update);
            tvQuotePrevClose = (TextView) findViewById(R.id.quote_prev_close);
            tvQuoteOpen = (TextView) findViewById(R.id.quote_open);
            tvQuoteLow = (TextView) findViewById(R.id.quote_low);
            tvQuoteHigh = (TextView)findViewById(R.id.quote_high);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        radioGroup.check(R.id.radio_1m);
        registerReceiver(connectivityReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void initFormats() {
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        priceFormat = (DecimalFormat)NumberFormat.getCurrencyInstance(Locale.US);
        priceFormat.setMaximumFractionDigits(2);
        priceFormat.setMinimumFractionDigits(2);
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    private void initActionBar() {
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initLoaderCallback(Bundle bundle) {
        destroyHistoryLoader();
        getSupportLoaderManager().initLoader(HISTORY_LOADER, bundle,
                historyLoaderCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        destroyHistoryLoader();
        unregisterReceiver(connectivityReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_KEY_SYMBOL, symbol);
    }

    private void destroyHistoryLoader() {
        if (getSupportLoaderManager().getLoader(HISTORY_LOADER) != null) {
            getSupportLoaderManager().getLoader(HISTORY_LOADER).cancelLoad();
            getSupportLoaderManager().destroyLoader(HISTORY_LOADER);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = Contract.Quote.COLUMN_SYMBOL + "=?";
        String[] selectionArgs = {symbol};
        return new CursorLoader(
                this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS_PROJECTION.toArray(new String[]{}),
                selection,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() != 0 && data.moveToFirst()) {
            updateDetailData(data);
        }
    }

    private void updateDetailData(Cursor data) {
        String name = data.getString(Contract.Quote.POSITION_COMPANY_NAME);
        actionBar.setTitle(name);

        if (isLandNotW820) {
            String symbol = data.getString(Contract.Quote.POSITION_SYMBOL);
            tvSymbol.setText(symbol);

            float price = data.getFloat(Contract.Quote.POSITION_PRICE);
            tvPrice.setText(String.format("$%s", price));

            float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
            if (rawAbsoluteChange > 0) {
                tvCurrentPrice.setTextColor(colorGreenA200);
            } else {
                tvCurrentPrice.setTextColor(colorPinkA100);
            }
            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);
            tvCurrentPrice.setText(String.format("%s (%s)", change, percentage));

            long dateLastUpdate = data.getLong(Contract.Quote.POSITION_DATE_TIME_UPDATE);
            tvLastUpdate.setText(getDateTimeToString(dateLastUpdate));

            float quotePrevClose = data.getFloat(Contract.Quote.POSITION_PREV_CLOSE);
            String quotePrevCloseToString = priceFormat.format(quotePrevClose);
            tvQuotePrevClose.setText(quotePrevCloseToString);

            float quoteOpen = data.getFloat(Contract.Quote.POSITION_OPEN);
            String quoteOpenToString = priceFormat.format(quoteOpen);
            tvQuoteOpen.setText(quoteOpenToString);

            float quoteLow = data.getFloat(Contract.Quote.POSITION_LOW);
            String quoteLowToString = priceFormat.format(quoteLow);
            tvQuoteLow.setText(quoteLowToString);

            float quoteHigh = data.getFloat(Contract.Quote.POSITION_HIGH);
            String quoteHighToString = priceFormat.format(quoteHigh);
            tvQuoteHigh.setText(quoteHighToString);
        }
    }

    private String getDateTimeToString(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_TIME_FORMATTER, Locale.US);
        return dateFormat.format(new Date(time));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private final LoaderManager.LoaderCallbacks<Cursor> historyLoaderCallback
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        Bundle bundle = new Bundle();

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (!args.isEmpty()) {
                bundle = args;
            }
            String selection = Contract.History.COLUMN_SYMBOL + "=?";
            String[] selectionArgs = {symbol};
            return new CursorLoader(
                    getApplicationContext(),
                    Contract.History.URI,
                    Contract.History.HISTORY_COLUMNS_PROJECTION.toArray(new String[]{}),
                    selection,
                    selectionArgs,
                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data.getCount() != 0 && data.moveToFirst()) {
                updateChartData(data, bundle);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void updateChartData(Cursor data, Bundle bundle) {
        int columnIndex;
        String formatter;
        int period = 0;
        if (bundle != null) {
            if (bundle.size() > 1) {
                period = bundle.getInt(KEY_PERIOD);
            }
            columnIndex = bundle.getInt(KEY_COLUMN_INDEX);
        } else {
            columnIndex = Contract.History.POSITION_HISTORY_ONE_MONTH;
        }

        switch (columnIndex) {
            case Contract.History.POSITION_HISTORY_ONE_MONTH:
                formatter = DAY_DATA_FORMATTER;
                break;
            case Contract.History.POSITION_HISTORY_SIX_MONTHS:
                formatter = MONTH_DATA_FORMATTER;
                break;
            case Contract.History.POSITION_HISTORY_MAX_YEARS:
                if (period == 1) {
                    formatter = MONTH_DATA_FORMATTER;
                } else {
                    formatter = YEAR_DATA_FORMATTER;
                }
                break;
            default:
                formatter = DAY_DATA_FORMATTER;
                break;
        }

        String history = data.getString(columnIndex);
        LineDataSet dataSet = new LineDataSet(EntryUtils.getEntries(history, period),
                getString(R.string.label_legend_chart));
        dataSet.setColor(colorGreenA200);
        dataSet.setDrawFilled(true);
        dataSet.setDrawValues(false);
        LineData lineData = new LineData(dataSet);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setTextColor(colorWhite);
        lineChart.setData(lineData);

        ChartMarkerView markerView = new ChartMarkerView(getApplicationContext(),
                R.layout.chart_marker);
        lineChart.setMarker(markerView);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(colorWhite);
        xAxis.setValueFormatter(new XFormatter(formatter));

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(colorWhite);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setTextColor(colorWhite);
        lineChart.animateX(getResources().getInteger(R.integer.duration_animate_x_chart));
    }
}
