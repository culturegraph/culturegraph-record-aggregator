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

    private Comparator<Subfield> createSubfieldComparator() {
        return new Comparator<Subfield>() {
            @Override
            public int compare(Subfield sf1, Subfield sf2) {
                int codeComparison = Integer.compare(sf1.getCode(), sf2.getCode());
                if (codeComparison != 0) return codeComparison;
                return sf1.getData().compareTo(sf2.getData());
            }
        };
    }

    @Override
    public int compare(DataField o1, DataField o2) {
        int tagComparison = o1.getTag().compareTo(o2.getTag());
        if (tagComparison != 0) return tagComparison;

        int ind1Comparison = Integer.compare(o1.getIndicator1(), o2.getIndicator1());
        if (ind1Comparison != 0) return ind1Comparison;

        int ind2Comparison = Integer.compare(o1.getIndicator2(), o2.getIndicator2());
        if (ind2Comparison != 0) return ind2Comparison;

        Iterator<Subfield> iter1 = o1.getSubfields().stream()
                .filter(sf -> sf.getCode() != ignoreSubfieldCode)
                .sorted(createSubfieldComparator())
                .iterator();

        Iterator<Subfield> iter2 = o2.getSubfields().stream()
                .filter(sf -> sf.getCode() != ignoreSubfieldCode)
                .sorted(createSubfieldComparator())
                .iterator();

        Comparator<Subfield> subfieldComparator = createSubfieldComparator();
        while(iter1.hasNext() && iter2.hasNext()) {
            Subfield sf1 = iter1.next();
            Subfield sf2 = iter2.next();

            int subfieldComparison = subfieldComparator.compare(sf1, sf2);
            if (subfieldComparison != 0) {
                return subfieldComparison;
            }
        }

        return 0;

    }
}
