package org.openrewrite.java;

import lombok.Value;

//Definition of new field
//Can we use JavaTemplate instead?
@Value
public class JavaFieldTemplate {
    String type;
    String name;
}
