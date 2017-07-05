package cf.javadev.stockhawk.ui;


import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class XFormatter implements IAxisValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Date date;

    XFormatter(String formatter) {
        this.dateFormat = new SimpleDateFormat(formatter, Locale.US);
        this.date = new Date();
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        date.setTime((long) value);
        return dateFormat.format(date);
    }
}
