package com.example.scoi.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberType {
    INDIVIDUAL("INDIVIDUAL"),
    CORPORATION("CORPORATION");

    private final String value;

    MemberType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MemberType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid MemberType: null");
        }

        switch (value.toLowerCase()) {
            case "individual":
            case "personal":
                return INDIVIDUAL;

            case "corporation":
            case "company":
                return CORPORATION;
        }

        throw new IllegalArgumentException("Invalid MemberType: " + value);
    }
}
