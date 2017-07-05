package cf.javadev.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.javadev.stockhawk.R;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import cf.javadev.stockhawk.data.Contract;
import cf.javadev.stockhawk.data.PrefUtils;
import cf.javadev.stockhawk.sync.QuoteSyncJob;
import cf.javadev.stockhawk.utils.ConnectivityUtil;
import timber.log.Timber;

import static android.support.design.widget.Snackbar.make;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {
    public static final String KEY_SYMBOL = "SYMBOL";
    private static final int STOCK_LOADER = 0;
    private static final String TAG_DIALOG_FRAGMENT = "StockDialogFragment";
    private static final String ERROR_MESSAGE = "cf.javadev.stockhawk.ERROR_MESSAGE";
    private static final String ACTION_DATA_UPDATED = "cf.javadev.stockhawk.ACTION_DATA_UPDATED";
    private static final String MESSAGE_TEXT = "message_text";
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    @BindView(R.id.fab)
    FloatingActionButton floatingActionButton;
    @BindView(R.id.root_main)
    CoordinatorLayout rootLayout;
    @BindColor(R.color.colorPrimary)
    int colorPrimary;

    private StockAdapter adapter;
    private Snackbar snackbar;
    private BroadcastReceiver connectivityReceiver = null;
    private BroadcastReceiver errorReceiver = null;

    @Override
    public void onClick(String symbol) {
        Timber.d("Symbol clicked: %s", symbol);
        if (symbol != null && !symbol.isEmpty()) {
            Intent intent = new Intent(this, StockDetailActivity.class);
            intent.putExtra(KEY_SYMBOL, symbol);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
                sendBroadcast(dataUpdatedIntent);
            }
        }).attachToRecyclerView(stockRecyclerView);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddStockDialog();
            }
        });
        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ConnectivityUtil.networkUp(getApplicationContext())) {
                    showSnackBarWithButtonSetting(R.string.message_no_connect);
                    floatingActionButton.setVisibility(View.GONE);
                } else {
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                    floatingActionButton.setVisibility(View.VISIBLE);
                }
            }
        };

        errorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String message = intent.getStringExtra(MESSAGE_TEXT);
                    showSnackBar(message);
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        };
    }

    private void showSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(colorPrimary);
        snackbar.show();
    }

    private void showAddStockDialog() {
        new AddStockDialog().show(getFragmentManager(), TAG_DIALOG_FRAGMENT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(connectivityReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(errorReceiver,
                new IntentFilter(ERROR_MESSAGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(connectivityReceiver);
        unregisterReceiver(errorReceiver);
    }

    @Override
    public void onRefresh() {
        QuoteSyncJob.syncImmediately(this);
        if (!ConnectivityUtil.networkUp(getApplicationContext()) && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.message_error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!ConnectivityUtil.networkUp(getApplicationContext())) {
            swipeRefreshLayout.setRefreshing(false);
            showSnackBarWithButtonSetting(R.string.message_refresh_failed);
        } else if (PrefUtils.getStocks(this).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.message_error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    private void showSnackBarWithButtonSetting(int messageId) {
        if (snackbar != null) {
            snackbar.dismiss();
        }
        String message = getString(messageId);
        snackbar = make(rootLayout, message,
                BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(colorPrimary);
        snackbar.setAction(R.string.action_setting, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        snackbar.show();
    }

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            if (ConnectivityUtil.networkUp(getApplicationContext())) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.message_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        } else {
            showSnackBar(getString(R.string.message_input_symbol));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS_PROJECTION.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);
        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
