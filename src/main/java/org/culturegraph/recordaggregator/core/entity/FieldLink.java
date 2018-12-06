package org.culturegraph.recordaggregator.core.entity;

import java.util.Objects;

/**
 * A value object that implements a container for a MARC21 field link.
 *
 * See also <a href="https://www.loc.gov/marc/bibliographic/ecbdcntf.html">Appendix A - Control Subfields</a> subsection
 * <i>$8 - Field link and sequence number</i>.
 */
public class FieldLink implements Comparable<FieldLink> {
    public final int number;
    public final String type;

    public FieldLink(int number, String type) {
        this.number = number;
        this.type = type;
    }

    public FieldLink increment() {
        return this.increment(1);
    }

    public FieldLink decrement() {
        return new FieldLink(Math.max(number - 1, 1), type);
    }

    public FieldLink increment(int addend) {
        return new FieldLink(number + addend, type);
    }

    public static FieldLink of(String s) {
        String[] token = s.split("\\\\", 2);
        String linking_number = token[0];

        int number;
        if (linking_number.contains(".")) {
            String linking_number_without_sequence_number = linking_number.split("\\.", 2)[0];
            number = Integer.parseInt(linking_number_without_sequence_number);
        } else {
            number = Integer.parseInt(linking_number);
        }

        String type = token[1];
        return new FieldLink(number, type);
    }

    public String asString() {
        return number + "\\" + type;
    }

    @Override
    public String toString() {
        return "FieldLink{" +
                "number=" + number +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldLink fieldLink = (FieldLink) o;
        return number == fieldLink.number &&
                Objects.equals(type, fieldLink.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, type);
    }

    @Override
    public int compareTo(FieldLink o) {
        return Integer.compare(number, o.number);
    }
}
