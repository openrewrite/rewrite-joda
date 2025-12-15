package org.openrewrite.java.joda.time;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.table.TypeMappings;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingLogger {
    private static final Logger log = LoggerFactory.getLogger(MappingLogger.class);

    private TypeMappings typeMappings;
    private ExecutionContext ctx;

    public MappingLogger(TypeMappings typeMappings) {
        this.typeMappings = typeMappings;
    }

    public void setExecutionContext(ExecutionContext ctx) {
        this.ctx = ctx;
    }

    public void info(String message, MethodCall method) {
        typeMappings.insertRow(ctx,
                new TypeMappings.Row(
                        method.getMethodType().getDeclaringType().toString(),
                        method.toString(),
                        message,
                        1,
                        null)
        );

        log.info(message + ": " + method);
    }
    public void info(String message, JavaType type) {
        typeMappings.insertRow(ctx,
                new TypeMappings.Row(
                        type.toString(),
                        type.toString(),
                        message,
                        1,
                        null)
        );

        log.info(message + ": " + type);
    }

}
