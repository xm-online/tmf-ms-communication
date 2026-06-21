package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import org.zalando.problem.Problem;
import org.zalando.problem.StatusType;
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
 * <p>It replaces the Jackson 2 {@code ProblemModule} shipped by {@code problem-spring-web},
 * which is not compatible with the {@code tools.jackson} (Jackson 3) mapper used after the
 * Spring Boot 4 migration. Without it {@link Problem} objects (which extend {@link Throwable})
 * are serialized as plain throwables and lose their RFC 7807 fields and custom parameters.
 */
public class ProblemModule extends SimpleModule {

    public ProblemModule() {
        super("ZalandoProblemModule");
        addSerializer(Problem.class, new ProblemSerializer());
    }

    static class ProblemSerializer extends ValueSerializer<Problem> {

        @Override
        public void serialize(Problem problem, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();

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
            for (Map.Entry<String, Object> entry : problem.getParameters().entrySet()) {
                gen.writePOJOProperty(entry.getKey(), entry.getValue());
            }

            gen.writeEndObject();
        }
    }
}