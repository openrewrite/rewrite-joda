/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.joda.time.templates;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;

import static java.util.Collections.emptyList;

public interface Templates {
    List<MethodTemplate> getTemplates();

    default List<FieldTemplate> getFields(){
        return emptyList();
    }

    /**
     * This method is used to disambiguate between multiple potential template matches for a given methodCall.
     * This should be overridden by Templates classes where methodMatcher.matches() may return more than one template.
     **/
    default boolean matchesMethodCall(Expression method, MethodTemplate template) {
        if (method instanceof J.NewClass) {
            return true;
        }

        Expression select = ((J.MethodInvocation) method).getSelect();
        if (select != null && select.getType() != null) {
            return select.getType().isAssignableFrom(template.getParentTypePattern());
        }

        return true;
    }

}
