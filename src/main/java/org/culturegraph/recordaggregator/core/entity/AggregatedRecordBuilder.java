package org.culturegraph.recordaggregator.core.entity;

import org.marc4j.marc.Record;

public interface AggregatedRecordBuilder {

    /** Adds a record to the record pool used for aggregation. */
    void add(Record record);

    /** Builds an aggregated record. */
    Record build();

    /** Resets the build. */
    void reset();
}
