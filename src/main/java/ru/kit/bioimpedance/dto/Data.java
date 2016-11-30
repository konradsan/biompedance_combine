package ru.kit.bioimpedance.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Inspection.class, name="Inspection"),
        @JsonSubTypes.Type(value = Inspections.class, name="Inspections"),
        @JsonSubTypes.Type(value = LastResearch.class, name="LastResearch"),
        @JsonSubTypes.Type(value = ReadyStatusBio.class, name="ReadyStatusBio"),
        @JsonSubTypes.Type(value = ReadyStatus.class, name="ReadyStatus"),
        @JsonSubTypes.Type(value = InspectionsTonometr.class, name="InspectionsTonometr"),
        @JsonSubTypes.Type(value = ReadyStatusTonometr.class, name="ReadyStatusTonometr"),
})
public abstract class Data {

}
