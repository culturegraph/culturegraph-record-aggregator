package org.culturegraph.recordaggregator.plugin;


import org.culturegraph.recordaggregator.core.entity.AggregatedRecordBuilder;

public class AggregatedRecordBuilderFactory {
    public static AggregatedRecordBuilder newBuilder() {
        return new AggregatedRecordBuilderImpl();
    }
}
