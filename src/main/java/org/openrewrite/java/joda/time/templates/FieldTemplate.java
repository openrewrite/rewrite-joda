package org.openrewrite.java.joda.time.templates;

import lombok.Value;
import org.openrewrite.java.FieldMatcher;
import org.openrewrite.java.JavaFieldTemplate;

@Value
public class FieldTemplate {
    FieldMatcher matcher;
    JavaFieldTemplate template;
}
