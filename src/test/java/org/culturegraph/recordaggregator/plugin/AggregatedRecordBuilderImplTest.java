package org.culturegraph.recordaggregator.plugin;

import helper.RecordBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.culturegraph.recordaggregator.core.entity.AggregatedRecordBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AggregatedRecordBuilderImplTest {

    private AggregatedRecordBuilderImpl aggregator;

    @Before
    public void setUp() throws Exception {
        aggregator = new AggregatedRecordBuilderImpl();
        aggregator.setBuildNumberPrefix("CG_");
        aggregator.setBuildNumberSuffix("_NOW");
        aggregator.setCatalogingAgency("DE-101");
    }

    @Test
    public void addFieldLinkToEachSubfieldForASingleRecord() {
        Record record = new RecordBuilder()
                .addControlField("001", "326216537")
                .addControlField("003", "DE-601")

                .addDataField("041", '0', ' ')
                .addSubfield('a', "eng")
                .build();

        aggregator.add(record);

        Record actual = aggregator.build();

        String id = actual.getControlNumber();
        assertThat(id, equalTo("CG_1_NOW"));

        DataField df035 = (DataField) actual.getVariableField("035");
        assertThat(df035, is(notNullValue()));

        Subfield sf035$a = df035.getSubfield('a');
        assertThat(sf035$a, is(notNullValue()));
        assertThat(sf035$a.getData(), is(equalTo("(DE-601)326216537")));

        Subfield sf035$8 = df035.getSubfield('8');
        assertThat(sf035$8, is(notNullValue()));
        assertThat(sf035$8.getData(), is(equalTo("1\\p")));


        DataField df041 = (DataField) actual.getVariableField("041");
        assertThat(df041, is(notNullValue()));

        Subfield sf041$8 = df041.getSubfield('8');
        assertThat(sf041$8, is(notNullValue()));
        assertThat(sf041$8.getData(), is(equalTo("1\\p")));
    }

    @Test
    public void checkAggregateControlNumber() {
        Record aggregate = aggregatedRecords();

        assertThat(aggregate.getControlNumber(), equalTo("CG_1_NOW"));

        ControlField cf003 = (ControlField) aggregate.getVariableField("003");
        assertThat(cf003.getData(), equalTo("DE-101"));
    }

    @Test
    public void checkAggregate035Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all035data = allSubfieldData(aggregate, "035");

        assertThat(all035data, hasSize(6));

        List<String> controlNumbers = Arrays.asList("(DE-601)326216537", "(DE-604)BV037374134", "(DE-101)454654678");
        assertThat(controlNumbers, everyItem(isIn(all035data)));

        List<String> fieldLinks = Arrays.asList("1\\p", "2\\p", "3\\p");
        assertThat(fieldLinks, everyItem(isIn(all035data)));
    }

    @Test
    public void checkAggregate041Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all041data = allSubfieldData(aggregate, "041");

        assertThat(all041data, hasSize(2));
        assertThat(all041data, hasItem("eng"));
        assertThat(all041data, hasItem("1\\p"));
    }

    @Test
    public void checkAggregate082Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all082data = allSubfieldData(aggregate, "082");

        assertThat(all082data, hasSize(2));
        assertThat(all082data, hasItem("200/.1"));
        assertThat(all082data, hasItem("1\\p"));
    }

    @Test
    public void checkAggregate084Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all084data = allSubfieldData(aggregate, "084");
        assertThat(all084data, hasSize(3));

        List<String> expected084data= Arrays.asList("CF 5017", "rvk");
        assertThat(expected084data, everyItem(isIn(all084data)));
        assertThat(all084data, hasItem("2\\p"));
    }

    @Test
    public void checkAggregate600Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all600data = allSubfieldData(aggregate, "600");
        assertThat(all600data, hasSize(6));

        List<String> expected600data= Arrays.asList("Kant, Immanuel", "1724-1804", "(DE-588)118559796", "gnd");
        assertThat(expected600data, everyItem(isIn(all600data)));

        List<String> fieldLinks = Arrays.asList("1\\p", "2\\p");
        assertThat(fieldLinks, everyItem(isIn(all600data)));
    }

    @Test
    public void checkAggregate650Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all650data = allSubfieldData(aggregate, "650");
        assertThat(all650data, hasSize(12));

        List<String> dataField650RecordOne = Arrays.asList("Religionsphilosophie", "(DE-588)4049415-9", "gnd");
        assertThat(dataField650RecordOne, everyItem(isIn(all650data)));


        List<String> firstDataField650RecordThree = Arrays.asList("Religion", "Philosophy");
        assertThat(firstDataField650RecordThree, everyItem(isIn(all650data)));

        List<String> secondDataField650RecordThree = Arrays.asList("(DE-588)4049415-9", "Religionsphilosophie", "gnd");
        assertThat(secondDataField650RecordThree, everyItem(isIn(all650data)));

        List<String> fieldLinks = Arrays.asList("1\\p", "3\\p", "4\\p");
        assertThat(fieldLinks, everyItem(isIn(all650data)));

        long fieldLinksToRecordOne = all650data.stream().filter(s -> s.equals("1\\p")).mapToInt(s -> 1).count();
        assertThat(fieldLinksToRecordOne, equalTo(1L));


        long fieldLinksToRecordThree = all650data.stream().filter(s -> s.equals("3\\p")).mapToInt(s -> 1).count();
        assertThat(fieldLinksToRecordThree, equalTo(2L));

        long provenanceLinksOfRecordThree = all650data.stream().filter(s -> s.equals("4\\p")).mapToInt(s -> 1).count();
        assertThat(provenanceLinksOfRecordThree, equalTo(1L));

    }

    @Test
    public void checkAggregate883Fields() {
        Record aggregate = aggregatedRecords();

        List<String> all883data = allSubfieldData(aggregate, "883");
        assertThat(all883data, hasSize(4));

        List<String> dataField883RecordThree = Arrays.asList("maschinell gebildet", "0,23683", "20180813");
        assertThat(dataField883RecordThree, everyItem(isIn(all883data)));

        assertThat(all883data, hasItem("4\\p"));
    }

    @Test
    public void shouldNotModifyDatafield035() {
        Record record = new RecordBuilder()
                .addControlField("001", "001")
                .addControlField("003", "003")
                .addDataField("035", ' ', ' ')
                .addSubfield('a', "035_a")
                .build();

        AggregatedRecordBuilderImpl builder = new AggregatedRecordBuilderImpl();
        builder.add(record);
        Record actual = builder.build();

        List<String> data = allSubfieldData(actual, "035");

        assertThat(data, hasSize(3));
        assertThat(data, containsInAnyOrder("035_a", "(003)001", "1\\p"));
    }

    @Test
    /**
     * A test to make sure that field links with sequence numbers are processable.
     */
    public void issueWithSequenceNumberThatLedToException() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("issueWithSequenceNumber.marcxml");
        MarcXmlReader reader = new MarcXmlReader(inputStream);

        AggregatedRecordBuilder builder = AggregatedRecordBuilderFactory.newBuilder();

        while (reader.hasNext()) {
            Record record = reader.next();
            builder.add(record);
        }

        builder.build();
    }

    @Test
    public void issueWithNonAggregatedDatafield689() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("issueDatafield689.marcxml");
        MarcXmlReader reader = new MarcXmlReader(inputStream);

        AggregatedRecordBuilder builder = AggregatedRecordBuilderFactory.newBuilder();

        while (reader.hasNext()) {
            Record record = reader.next();
            builder.add(record);
        }

        Record result = builder.build();
        DataField df689 = (DataField) result.getVariableField("689");
        List<Subfield> subfields = df689.getSubfields('8');

        assertThat(subfields, hasSize(2));

        List<String> data = subfields.stream().map(Subfield::getData).collect(Collectors.toList());
        assertThat(data, containsInAnyOrder("1\\p", "2\\p"));
    }

    @Test
    public void shouldAlsoIncrementNonProvenanceFields() {
        Record record1 = new RecordBuilder()
                .addControlField("001", "326216537")
                .addControlField("003", "DE-601")

                .addDataField("041", '0', ' ')
                .addSubfield('8', "1\\u")
                .addSubfield('a', "eng")

                .build();

        Record record2 = new RecordBuilder()
                .addControlField("001", "326216537-2")
                .addControlField("003", "DE-601")

                .addDataField("041", '0', ' ')
                .addSubfield('8', "1\\u")
                .addSubfield('a', "eng")

                .build();

        AggregatedRecordBuilder builder = AggregatedRecordBuilderFactory.newBuilder();

        builder.add(record1);
        builder.add(record2);
        Record result = builder.build();

        DataField df = (DataField)  result.getVariableField("041");
        assertThat(df.toString(), startsWith("041 0 "));
        assertThat(df.toString(), containsString("$aeng"));
        assertThat(df.toString(), containsString("$81\\p"));
        assertThat(df.toString(), containsString("$82\\p"));
        assertThat(df.toString(), containsString("$81\\u"));
        assertThat(df.toString(), containsString("$82\\u"));
    }

    @Ignore
    @Test
    public void printAggregatedRecordAsMARCXML() throws Exception {
        AggregatedRecordBuilderImpl aggregator = new AggregatedRecordBuilderImpl();
        aggregator.setBuildNumberPrefix("CG_");

        LocalDateTime ltd = LocalDateTime.now();
        ZonedDateTime utc = ltd.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String suffix = formatter.format(utc);

        aggregator.setBuildNumberSuffix("_" + suffix);
        aggregator.setCatalogingAgency("DE-101");

        boolean indent = true;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MarcXmlWriter writer = new MarcXmlWriter(outputStream, indent);

        aggregator.add(createRecordOne());
        aggregator.add(createRecordTwo());
        aggregator.add(createRecordThree());

        writer.write(aggregator.build());

        writer.close();
        outputStream.close();

        String marcxml = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(marcxml);
    }

    private List<String> allSubfieldData(Record record, String tag) {
        return  record.getVariableFields(tag).stream()
                .map(DataField.class::cast)
                .map(DataField::getSubfields)
                .flatMap(List::stream)
                .map(Subfield::getData)
                .collect(Collectors.toList());
    }

    private Record aggregatedRecords() {
        Record one = createRecordOne();
        Record two = createRecordTwo();
        Record three = createRecordThree();

        aggregator.add(one);
        aggregator.add(two);
        aggregator.add(three);

        return aggregator.build();
    }

    private Record createRecordOne() {
        return new RecordBuilder()
                .addControlField("001", "326216537")
                .addControlField("003", "DE-601")

                .addDataField("041", '0', ' ')
                .addSubfield('a', "eng")

                .addDataField("082", '0', '0')
                .addSubfield('a', "200/.1")

                .addDataField("600", '1', '7')
                .addSubfield('a', "Kant, Immanuel")
                .addSubfield('d', "1724-1804")
                .addSubfield('0', "(DE-588)118559796")
                .addSubfield('2', "gnd")

                .addDataField("650", '0', '7')
                .addSubfield('a', "Religionsphilosophie")
                .addSubfield('0', "(DE-588)4049415-9")
                .addSubfield('2', "gnd")

                .build();
    }

    private Record createRecordTwo() {
        return new RecordBuilder()
                .addControlField("001", "BV037374134")
                .addControlField("003", "DE-604")

                .addDataField("084", ' ', ' ')
                .addSubfield('a', "CF 5017")
                .addSubfield('2', "rvk")

                .addDataField("600", '1', '7')
                .addSubfield('a', "Kant, Immanuel")
                .addSubfield('d', "1724-1804")
                .addSubfield('0', "(DE-588)118559796")
                .addSubfield('2', "gnd")

                .build();
    }

    private Record createRecordThree() {
        return new RecordBuilder()
                .addControlField("001", "454654678")
                .addControlField("003", "DE-101")

                .addDataField("650", ' ', '0')
                .addSubfield('a', "Religion")
                .addSubfield('x', "Philosophy")

                .addDataField("650", ' ', '7')
                .addSubfield('a', "Religionsphilosophie")
                .addSubfield('0', "(DE-588)4049415-9")
                .addSubfield('2', "gnd")
                .addSubfield('8', "1\\p")

                .addDataField("883", '0', ' ')
                .addSubfield('a', "maschinell gebildet")
                .addSubfield('c', "0,23683")
                .addSubfield('d', "20180813")
                .addSubfield('8', "1\\p")

                .build();
    }
}