package ru.kit.bioimpedance.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Launch.class, name="LAUNCH"),
        @JsonSubTypes.Type(value = CheckStatus.class, name="CHECK_STATUS"),
        @JsonSubTypes.Type(value = StartTest.class, name="START_TEST"),
        @JsonSubTypes.Type(value = GetLastResearch.class, name="GET_LAST_RESEARCH"),
        @JsonSubTypes.Type(value = StopTest.class, name="STOP_TEST"),

})
abstract public class Command {

}
