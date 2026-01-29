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
        for (MemberType type : MemberType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid MemberType: " + value);
    }
}
