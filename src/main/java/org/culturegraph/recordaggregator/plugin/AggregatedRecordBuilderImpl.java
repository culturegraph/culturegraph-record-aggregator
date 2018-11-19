package org.culturegraph.recordaggregator.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.culturegraph.recordaggregator.core.entity.FieldLink;
import org.culturegraph.recordaggregator.core.entity.AggregatedRecordBuilder;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class AggregatedRecordBuilderImpl implements AggregatedRecordBuilder {

    private int buildNumber;
    private String prefix;
    private String suffix;
    private String catalogingAgency;

    private List<Record> records;
    private FieldLink lastFieldLink;
    private MarcFactory factory;
    private DataFieldComparator comparator;

    public AggregatedRecordBuilderImpl() {
        this.buildNumber = 0;
        this.prefix = "";
        this.suffix = "";

        this.records = new ArrayList<>();
        this.factory = MarcFactory.newInstance();
        this.comparator = new DataFieldComparator('8');
        this.lastFieldLink = new FieldLink(0, "p");

        this.catalogingAgency = "";
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setBuildNumberPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setBuildNumberSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setCatalogingAgency(String catalogingAgency) {
        this.catalogingAgency = catalogingAgency;
    }

    @Override
    public void add(Record record) {
        linkEachDataFieldToIdn(record);

        if (records.isEmpty()) {
            Optional<FieldLink> opt = findMaxFieldLink(record.getDataFields());
            if (!opt.isPresent()) throw new IllegalArgumentException("Could not find any field link (subfield $8) in record.");
            lastFieldLink = opt.get();
        } else {
            Optional<FieldLink> opt = incrementAllFieldLinks(record.getDataFields(), lastFieldLink.number);
            if (!opt.isPresent()) throw new IllegalArgumentException("Could not find any field link (subfield $8) in record.");
            lastFieldLink = opt.get();
        }

        records.add(record);
    }

    @Override
    public Record build() {
        buildNumber += 1;

        Record result = factory.newRecord();

        List<DataField> allDataFields = records.stream()
                .map(Record::getDataFields)
                .flatMap(List::stream)
                .sorted()
                .collect(Collectors.toList());

        DataField lastDataField = null;
        for (DataField df: allDataFields) {
            if (lastDataField == null) {
                lastDataField = df;
                continue;
            }

            if (comparator.compare(lastDataField, df) == 0) {
                lastDataField.addSubfield(df.getSubfield('8'));
            } else {
                result.addVariableField(lastDataField);
                lastDataField = df;
            }
        }

        result.addVariableField(lastDataField);

        // Purge stored records
        records = new ArrayList<>();
        // Reset max field link
        lastFieldLink = new FieldLink(0, "p");

        String id = prefix.isEmpty() && suffix.isEmpty() ? String.valueOf(buildNumber) : prefix + buildNumber + suffix;
        result.addVariableField(factory.newControlField("001", id));
        result.addVariableField(factory.newControlField("003", catalogingAgency));

        return result;
    }

    @Override
    public void reset() {
        buildNumber = 0;
        records = new ArrayList<>();
        lastFieldLink = new FieldLink(0, "p");
    }

    /**
     * Increments each field link (subfield 8 in a data field) by a constant <i>addend</i> among all data fields, if present.
     * @return Field link with the highest sequence number among all data fields after the modification, if present.
     */
    private Optional<FieldLink> incrementAllFieldLinks(List<DataField> dataFields, int addend) {
        FieldLink max = null;

        for (DataField df: dataFields) {
            for (Subfield sf: df.getSubfields('8')) {
                FieldLink fl = FieldLink.of(sf.getData()).increment(addend);

                if (max == null) {
                    max = fl;
                } else {
                    max = max.compareTo(fl) > 0 ? max : fl;
                }

                sf.setData(fl.asString());
            }
        }

        return Optional.ofNullable(max);
    }

    /**
     * Finds the field link (subfield 8 in a data field) with the highest sequence number among all data fields, if present.
     */
    private Optional<FieldLink> findMaxFieldLink(List<DataField> dataFields) {
        FieldLink max = null;

        for (DataField df: dataFields) {
            for (Subfield sf: df.getSubfields('8')) {
                FieldLink fl = FieldLink.of(sf.getData());
                if (max == null) {
                    max = fl;
                } else {
                    max = max.compareTo(fl) > 0 ? max : fl;
                }
            }
        }

        return Optional.ofNullable(max);
    }

    /**
     * Reads field <i>001</i> (id) and <i>003</i> (cataloging agency) to create a idn that looks as follows <code>(AGENCY)ID</code>.
     * <P> If <i>003</i> is missing, the field <i>040$a</i> is used for the cataloging agency.
     * <P> If <i>040$a</i> is missing, the value <i>Undefined</i> is used.
     * @return A composite id.
     */
    private String createIdn(Record record) {
        String id = "";
        // International Standard Identifier for Libraries and Related Organizations
        String isil = "";
        for (ControlField cf: record.getControlFields()) {
            String tag = cf.getTag();
            if (tag.equals("001")) {
                id = cf.getData();
            }
            if (tag.equals("003")) {
                isil = cf.getData();
                break;
            }
        }

        if (isil.isEmpty()) {
            for (DataField df: record.getDataFields()) {
                String tag = df.getTag();
                if (tag.equals("040")) {
                    Subfield sf = df.getSubfield('a');
                    if (sf != null) {
                        isil = sf.getData();
                    }
                }
                if (tag.compareTo("040") > 0) {
                    break;
                }
            }
        }

        if (isil.isEmpty()) {
            isil = "Undefined";
        }

        return "(" + isil + ")" + id;
    }

    /**
     * Appends the data field <i>035</i> with the idn in subfield <i>a</i> and a unique field link in subfield <i>8</i>.
     * <P> Appends the subfield 8 to each data field that does not starts with 8XX.
     * <P> The subfield contains the reference to the created <i>035</i> field.
     *
     *
     * @param record A record to modify.
     */
    private void linkEachDataFieldToIdn(Record record) {
        String idn = createIdn(record);

        FieldLink recordFieldLink = new FieldLink(1, "p");

        for (DataField df: record.getDataFields()) {
            List<Subfield> subfields = df.getSubfields('8');

            boolean isProvenanceField = df.getTag().startsWith("883");
            if (!isProvenanceField) {
                df.addSubfield(factory.newSubfield('8', recordFieldLink.asString()));
            }

            for (Subfield sf: subfields) {
                String data = sf.getData();
                sf.setData(FieldLink.of(data).increment(1).asString());
            }
        }

        DataField systemControlNumber = factory.newDataField("035", ' ', ' ');
        systemControlNumber.addSubfield(factory.newSubfield('a', idn));
        systemControlNumber.addSubfield(factory.newSubfield('8', recordFieldLink.asString()));
        record.addVariableField(systemControlNumber);
    }
}
