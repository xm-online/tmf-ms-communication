package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import org.zalando.problem.Problem;
import org.zalando.problem.StatusType;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.net.URI;
import java.util.Map;

/**
 * Jackson 3 module that serializes Zalando {@link Problem} instances following RFC 7807.
 *
 * <p>It replaces the Jackson 2 modules shipped by {@code problem-spring-web}
 * ({@code ProblemModule} and {@code ConstraintViolationProblemModule}), which are not compatible
 * with the {@code tools.jackson} (Jackson 3) mapper used after the Spring Boot 4 migration.
 * Without it {@link Problem} objects (which extend {@link Throwable}) are serialized as plain
 * throwables and lose their RFC 7807 fields and custom parameters, and
 * {@link ConstraintViolationProblem}/{@link Violation} lose their {@code violations} payload.
 */
public class ProblemModule extends SimpleModule {

    public ProblemModule() {
        super("ZalandoProblemModule");
        addSerializer(Problem.class, new ProblemSerializer());
        addSerializer(ConstraintViolationProblem.class, new ConstraintViolationProblemSerializer());
        addSerializer(Violation.class, new ViolationSerializer());
    }

    private static void writeProblemFields(Problem problem, JsonGenerator gen) {
        URI type = problem.getType();
        if (type != null && !Problem.DEFAULT_TYPE.equals(type)) {
            gen.writeStringProperty("type", type.toString());
        }
        if (problem.getTitle() != null) {
            gen.writeStringProperty("title", problem.getTitle());
        }
        StatusType status = problem.getStatus();
        if (status != null) {
            gen.writeNumberProperty("status", status.getStatusCode());
        }
        if (problem.getDetail() != null) {
            gen.writeStringProperty("detail", problem.getDetail());
        }
        if (problem.getInstance() != null) {
            gen.writeStringProperty("instance", problem.getInstance().toString());
        }
    }

    static class ProblemSerializer extends ValueSerializer<Problem> {

        @Override
        public void serialize(Problem problem, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            writeProblemFields(problem, gen);
            for (Map.Entry<String, Object> entry : problem.getParameters().entrySet()) {
                gen.writePOJOProperty(entry.getKey(), entry.getValue());
            }
            gen.writeEndObject();
        }
    }

    static class ConstraintViolationProblemSerializer extends ValueSerializer<ConstraintViolationProblem> {

        @Override
        public void serialize(ConstraintViolationProblem problem, JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException {
            gen.writeStartObject();
            writeProblemFields(problem, gen);
            gen.writePOJOProperty("violations", problem.getViolations());
            gen.writeEndObject();
        }
    }

    static class ViolationSerializer extends ValueSerializer<Violation> {

        @Override
        public void serialize(Violation violation, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("field", violation.getField());
            gen.writeStringProperty("message", violation.getMessage());
            gen.writeEndObject();
        }
    }
}
