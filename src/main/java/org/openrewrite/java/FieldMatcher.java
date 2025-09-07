package org.openrewrite.java;

import lombok.Value;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
public class FieldMatcher {
    String type;
    String name;

    public boolean match(J.FieldAccess filed) {
        String fieldType = ((JavaType.Class) filed.getType()).getFullyQualifiedName();
        String fieldName = filed.getName().getSimpleName();

        if (!type.equals(fieldType)) return false;
        if (!name.equals(fieldName)) return false;

        return true;
    }
}
