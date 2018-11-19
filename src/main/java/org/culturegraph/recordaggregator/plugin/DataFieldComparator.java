package org.culturegraph.recordaggregator.plugin;

import java.util.Comparator;
import java.util.Iterator;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;

public class DataFieldComparator implements Comparator<DataField> {

    private char ignoreSubfieldCode;

    public DataFieldComparator(char ignoreSubfieldCode) {
        this.ignoreSubfieldCode = ignoreSubfieldCode;
    }

    @Override
    public int compare(DataField o1, DataField o2) {
        if (o1.compareTo(o2) != 0) return -1;

        Iterator<Subfield> iter1 = o1.getSubfields().stream().filter(sf -> sf.getCode() != ignoreSubfieldCode).iterator();
        Iterator<Subfield> iter2 = o2.getSubfields().stream().filter(sf -> sf.getCode() != ignoreSubfieldCode).iterator();

        while(iter1.hasNext() && iter2.hasNext()) {
            Subfield sf1 = iter1.next();
            Subfield sf2 = iter2.next();

            if (sf1.getCode() != sf2.getCode() || !sf1.getData().equals(sf2.getData())) {
                return -1;
            }
        }

        return 0;

    }
}
