package cf.javadev.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.javadev.stockhawk.R;

import cf.javadev.stockhawk.ui.MainActivity;
import cf.javadev.stockhawk.ui.StockDetailActivity;

public class StockWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_DATA_UPDATED = "cf.javadev.stockhawk.ACTION_DATA_UPDATED";
    private static final String LOG_TAG = "my";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_widget);

            Intent intentMain = new Intent(context, MainActivity.class);
            PendingIntent clickBarPendingIntent = PendingIntent.getActivity(context, 0, intentMain, 0);
            views.setOnClickPendingIntent(R.id.widget_bar, clickBarPendingIntent);

            views.setRemoteAdapter(R.id.widget_list,
                    new Intent(context, StockWidgetService.class));

            Intent intentItem = new Intent(context, StockDetailActivity.class);
            PendingIntent clickItemPendingIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(intentItem)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list, clickItemPendingIntent);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_DATA_UPDATED.equals(intent.getAction())) {
            Log.d(LOG_TAG, "StockWidgetProvider,  onReceive: ");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }
}
