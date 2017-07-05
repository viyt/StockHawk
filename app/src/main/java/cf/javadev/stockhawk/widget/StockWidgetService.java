package cf.javadev.stockhawk.widget;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.javadev.stockhawk.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import cf.javadev.stockhawk.data.Contract;

public class StockWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private static final String KEY_SYMBOL = "SYMBOL";
            private Cursor data;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(
                        Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS_PROJECTION.toArray(new String[]{}),
                        null,
                        null,
                        Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @SuppressLint("PrivateResource")
            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null
                        || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.list_item_quote);

                String stockSymbol = data.getString(Contract.Quote.POSITION_SYMBOL);
                Float stockPrice = data.getFloat(Contract.Quote.POSITION_PRICE);
                Float absoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                int backgroundDrawable;

                DecimalFormat dollarFormat = (DecimalFormat) NumberFormat
                        .getCurrencyInstance(Locale.US);
                DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat
                        .getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                dollarFormatWithPlus.setMaximumFractionDigits(2);
                dollarFormat.setMaximumFractionDigits(2);
                dollarFormat.setMinimumFractionDigits(2);
                dollarFormatWithPlus.setMinimumFractionDigits(2);

                if (absoluteChange > 0) {
                    backgroundDrawable = R.drawable.percent_change_pill_green;
                } else {
                    backgroundDrawable = R.drawable.percent_change_pill_red;
                }

                views.setTextViewText(R.id.symbol, stockSymbol);
                views.setTextViewText(R.id.price, dollarFormat.format(stockPrice));
                views.setTextViewText(R.id.change, dollarFormatWithPlus.format(absoluteChange));
                views.setInt(R.id.change, "setBackgroundResource", backgroundDrawable);
                views.setInt(R.id.item_list_quote, "setBackgroundResource",
                        R.color.material_grey_850);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(KEY_SYMBOL, stockSymbol);
                views.setOnClickFillInIntent(R.id.item_list_quote, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.list_item_quote);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return data.moveToPosition(position)
                        ? data.getLong(Contract.Quote.POSITION_ID) : position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
