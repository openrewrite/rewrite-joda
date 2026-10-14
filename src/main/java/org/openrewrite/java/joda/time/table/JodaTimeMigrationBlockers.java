/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.joda.time.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class JodaTimeMigrationBlockers extends DataTable<JodaTimeMigrationBlockers.Row> {

    public JodaTimeMigrationBlockers(Recipe recipe) {
        super(recipe,
                "Joda-Time migration blockers",
                "Source files that were left on Joda-Time, and why they could not be migrated to `java.time`.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The source file that was left on Joda-Time.")
        String sourcePath;

        @Column(displayName = "Reason",
                description = "Why the source file could not be migrated to `java.time`.")
        String reason;
    }
}
