package cf.javadev.stockhawk.ui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.javadev.stockhawk.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("ViewConstructor")
public class ChartMarkerView extends MarkerView {
    private static final String DATE_FORMATTER = "MM-dd-yyyy";
    private TextView tvMarker;
    private final DecimalFormat priceFormat;
    private final int uiScreenWidth;

    /**
     * Constructor. Sets up the MarkerView with a custom layout resource.
     *
     * @param context        the Application context
     * @param layoutResource the layout resource to use for the MarkerView
     */
    public ChartMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvMarker = (TextView) findViewById(R.id.marker);
        priceFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
        uiScreenWidth = getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        float stockPrice = e.getY();
        String stockPriceToString = priceFormat.format(stockPrice);
        long date = (long) e.getX();
        tvMarker.setText(String.format("%s, %s", stockPriceToString, getDateToString(date)));
        super.refreshContent(e, highlight);
    }

    private String getDateToString(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMATTER, Locale.getDefault());
        return dateFormat.format(new Date(time));
    }

    /**
     * This method translate to the correct position X and draw
     */
    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        int width = getWidth();
        if ((uiScreenWidth - posX - width) < width) {
            posX -= width;
        }
        canvas.translate(posX, posY);
        draw(canvas);
        canvas.translate(-posX, -posY);
    }
}
