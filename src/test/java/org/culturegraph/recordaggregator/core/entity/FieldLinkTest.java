package org.culturegraph.recordaggregator.core.entity;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class FieldLinkTest {
    @Test
    public void shouldMapFieldLink() throws Exception {
        FieldLink fl = FieldLink.of("1\\p");
        assertThat(fl.number, equalTo(1));
        assertThat(fl.type, equalTo("p"));
    }

    @Test
    public void shouldIgnoreSequenceNumberFieldLink() throws Exception {
        FieldLink fl = FieldLink.of("1.1\\p");
        assertThat(fl.number, equalTo(1));
        assertThat(fl.type, equalTo("p"));
    }
}