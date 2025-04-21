package scatterchat.utils;

import java.util.List;

import com.sarojaba.prettytable4j.PrettyTable;

import scatterchat.protocol.message.cyclon.CyclonEntry;


public final class PrettyPrint {

    public static String CyclonEntriestoString(List<CyclonEntry> data, String header1, String header2, String header3) {

        PrettyTable pt = PrettyTable.fieldNames(header1, header2, header3);

        for (CyclonEntry entry : data) {
            pt.addRow(
                entry.identity(),
                entry.pubAddress(),
                entry.pubTimerAddress()
            );
        }

        return pt.toString();
    }
}