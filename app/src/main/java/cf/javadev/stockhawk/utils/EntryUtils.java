package cf.javadev.stockhawk.utils;


import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class EntryUtils {

    public static List<Entry> getEntries(String history, int period) {
        final long compareDate = lookingDate(period);
        String[] historyData = history.split("\\n");
        List<Entry> entryList = new ArrayList<>();
        for (String aData : historyData) {
            String[] pair = aData.split(",");
            if (period != 0) {
                if (Long.parseLong(pair[0]) > compareDate) {
                    entryList.add(new Entry(Float.parseFloat(pair[0].trim()),
                            Float.parseFloat(pair[1].trim())));
                }
            } else {
                entryList.add(new Entry(Float.parseFloat(pair[0].trim()),
                        Float.parseFloat(pair[1].trim())));
            }
        }
        Collections.sort(entryList,new EntryXComparator());
        return entryList;
    }

    private static long lookingDate(int period) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.MILLISECOND);
        calendar.add(Calendar.YEAR, -period);
        return calendar.getTimeInMillis();
    }
}
