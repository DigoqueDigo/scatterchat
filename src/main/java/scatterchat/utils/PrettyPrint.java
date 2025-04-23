package scatterchat.utils;

import java.util.List;

import com.sarojaba.prettytable4j.PrettyTable;

import scatterchat.protocol.message.cyclon.CyclonEntry;


public final class PrettyPrint {

    public static String CyclonEntriestoString(List<CyclonEntry> data, String header) {

        PrettyTable pt = PrettyTable.fieldNames(header);

        for (CyclonEntry entry : data) {
            pt.addRow(entry.pullAddress());
        }

        return pt.toString();
    }
}