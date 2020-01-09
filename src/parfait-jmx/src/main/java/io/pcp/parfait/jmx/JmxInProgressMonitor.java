/*
 * Copyright 2009-2017 Aconex
 *
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.pcp.parfait.jmx;

import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import io.pcp.parfait.timing.InProgressExporter;
import io.pcp.parfait.timing.InProgressSnapshot;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@ManagedResource
public class JmxInProgressMonitor {
    private static final Function<Class<?>, OpenType<?>> CLASS_TO_OPENTYPE = new Function<Class<?>, OpenType<?>>() {
        private final Map<Class<?>, SimpleType<?>> mappings = ImmutableMap
                .<Class<?>, SimpleType<?>> of(String.class, SimpleType.STRING, Long.class,
                        SimpleType.LONG);

        @Override
        public OpenType<?> apply(Class<?> from) {
            return mappings.get(from);
        }
    };
    
    static final Function<InProgressSnapshot, TabularData> TO_TABULAR_DATA = new Function<InProgressSnapshot, TabularData>() {
        @Override
        public TabularData apply(InProgressSnapshot from) {
            List<OpenType<?>> types = Lists.transform(from.getColumnClasses(), CLASS_TO_OPENTYPE);

            CompositeType rowType;
            try {
                int columnCount = from.getColumnCount();
                rowType = new CompositeType("Snapshot row", "Snapshot row", from.getColumnNames()
                        .toArray(new String[columnCount]), from.getColumnDescriptions().toArray(
                        new String[columnCount]), types.toArray(new OpenType<?>[columnCount]));
                TabularType type = new TabularType("Snapshot", "Snapshot", rowType,
                        new String[] { "Thread name" });
                TabularData data = new TabularDataSupport(type);

                for (Map<String, Object> dataRow : from.getValues()) {
                    CompositeData row = new CompositeDataSupport(rowType, dataRow);
                    data.put(row);
                }
                return data;
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }
    };
    
    private final InProgressExporter exporter;

    public JmxInProgressMonitor(InProgressExporter exporter) {
        this.exporter = exporter;
    }
    
    @ManagedAttribute
    public TabularData getInProgressOperations() {
        return TO_TABULAR_DATA.apply(exporter.getSnapshot());
    }

    @ManagedAttribute
    public String getSnapshotAsString() {
        return exporter.getSnapshot().asTabbedString();
    }

    @ManagedAttribute
    public String getFormattedSnapshot() {
        return exporter.getSnapshot().asFormattedString();
    }
}
