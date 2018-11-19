package helper;

import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class RecordBuilder {

    private MarcFactory factory;
    private Record record;
    private DataField dataField;

    public RecordBuilder() {
        this.factory = MarcFactory.newInstance();
        this.record = factory.newRecord("00000cam a2200000 a 4500");
    }

    public RecordBuilder addControlField(String tag, String data) {
        record.addVariableField(factory.newControlField(tag, data));
        return this;
    }

    public RecordBuilder addDataField(String tag, char ind1, char ind2) {
        if (dataField != null) {
            record.addVariableField(dataField);
            dataField = null;
        }
        dataField = factory.newDataField(tag, ind1, ind2);
        return this;
    }

    public RecordBuilder addSubfield(char code, String data) {
        Subfield sf = factory.newSubfield(code, data);
        dataField.addSubfield(sf);
        return this;
    }

    public Record build() {
        if (dataField != null) {
            record.addVariableField(dataField);
            dataField = null;
        }
        return record;
    }

}
