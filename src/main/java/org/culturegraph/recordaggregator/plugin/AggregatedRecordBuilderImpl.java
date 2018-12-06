package org.culturegraph.recordaggregator.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.culturegraph.recordaggregator.core.entity.AggregatedRecordBuilder;
import org.culturegraph.recordaggregator.core.entity.FieldLink;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class AggregatedRecordBuilderImpl implements AggregatedRecordBuilder {

    private static String PROVENANCE = "p";
    private static char FIELD_LINK_CODE = '8';

    private int buildNumber;
    private String prefix;
    private String suffix;
    private String catalogingAgency;

    private MarcFactory factory;
    private List<Record> records;
    private DataFieldComparator dataFieldComparator;
    private Map<String,FieldLink> lastFieldLinks;

    public AggregatedRecordBuilderImpl() {
        this.buildNumber = 0;
        this.prefix = "";
        this.suffix = "";
        this.catalogingAgency = "";

        this.factory = MarcFactory.newInstance();
        this.records = new ArrayList<>();
        this.dataFieldComparator = new DataFieldComparator(FIELD_LINK_CODE);
        /** Contains the last field link for each type */
        this.lastFieldLinks = new HashMap<>();
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
            findMaxFieldLink(record.getDataFields(), lastFieldLinks);
        } else {
            incrementAllFieldLinks(record.getDataFields(), lastFieldLinks);
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
                .sorted(dataFieldComparator)
                .collect(Collectors.toList());

        DataField lastDataField = null;
        for (DataField df: allDataFields) {
            if (lastDataField == null) {
                lastDataField = df;
                continue;
            }

            if (dataFieldComparator.compare(lastDataField, df) == 0) {
                df.getSubfields(FIELD_LINK_CODE).forEach(lastDataField::addSubfield);
            } else {
                result.addVariableField(lastDataField);
                lastDataField = df;
            }
        }

        result.addVariableField(lastDataField);

        // Purge stored records
        records = new ArrayList<>();
        // Reset last field links
        lastFieldLinks = new HashMap<>();

        String id = prefix.isEmpty() && suffix.isEmpty() ? String.valueOf(buildNumber) : prefix + buildNumber + suffix;
        result.addVariableField(factory.newControlField("001", id));
        result.addVariableField(factory.newControlField("003", catalogingAgency));

        return result;
    }

    @Override
    public void reset() {
        buildNumber = 0;
        records = new ArrayList<>();
        lastFieldLinks = new HashMap<>();
    }

    /**
     * Increments each field link (subfield 8 in a data field) by a constant <i>addend</i>.
     * @return Field link with the highest sequence number among all data fields after the modification, if present.
     */
    private void incrementAllFieldLinks(List<DataField> dataFields, Map<String,FieldLink> lastFieldLinks) {
        // The *addend* is the highest field number from the previous record in the records list.
        Map<String,Integer> addends = lastFieldLinks.entrySet().stream()
                .collect(Collectors.toMap(
                   e -> e.getKey(),
                   e -> e.getValue().number
                ));

        for (DataField df: dataFields) {
            for (Subfield sf: df.getSubfields(FIELD_LINK_CODE)) {
                FieldLink fl = FieldLink.of(sf.getData());
                int addend = addends.getOrDefault(fl.type, 0);
                fl = fl.increment(addend);
                String type = fl.type;

                if (lastFieldLinks.getOrDefault(type, null) == null) {
                    lastFieldLinks.put(type, fl);
                } else {
                    FieldLink maximum = lastFieldLinks.get(type);
                    maximum = maximum.compareTo(fl) > 0 ? maximum : fl;
                    lastFieldLinks.put(type, maximum);
                }

                sf.setData(fl.asString());
            }
        }
    }

    /**
     * Finds the field link (subfield 8 in a data field) with the highest sequence number among all data fields, if present.
     */
    private void findMaxFieldLink(List<DataField> dataFields, Map<String,FieldLink> lastFieldLinks) {
        for (DataField df: dataFields) {
            for (Subfield sf: df.getSubfields(FIELD_LINK_CODE)) {
                FieldLink fl = FieldLink.of(sf.getData());
                String type = fl.type;

                if (lastFieldLinks.getOrDefault(type, null) == null) {
                    lastFieldLinks.put(type, fl);
                } else {
                    FieldLink maximum = lastFieldLinks.get(type);
                    maximum = maximum.compareTo(fl) > 0 ? maximum : fl;
                    lastFieldLinks.put(type, maximum);
                }
            }
        }
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

        FieldLink recordFieldLink = new FieldLink(1, PROVENANCE);

        for (DataField df: record.getDataFields()) {
            List<Subfield> subfields = df.getSubfields(FIELD_LINK_CODE);

            String tag = df.getTag();
            if (!tag.equals("035") && !tag.equals("883")) {
                df.addSubfield(factory.newSubfield(FIELD_LINK_CODE, recordFieldLink.asString()));
            }

            for (Subfield sf: subfields) {
                String data = sf.getData();
                FieldLink fl = FieldLink.of(data);
                if (fl.type.equals(PROVENANCE)) {
                    sf.setData(fl.increment(1).asString());
                }
            }
        }

        DataField systemControlNumber = factory.newDataField("035", ' ', ' ');
        systemControlNumber.addSubfield(factory.newSubfield('a', idn));
        systemControlNumber.addSubfield(factory.newSubfield(FIELD_LINK_CODE, recordFieldLink.asString()));
        record.addVariableField(systemControlNumber);
    }
}
