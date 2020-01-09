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

package io.pcp.parfait.dropwizard;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import io.pcp.parfait.dropwizard.metricadapters.CountingAdapter;
import io.pcp.parfait.dropwizard.metricadapters.GaugeAdapter;
import io.pcp.parfait.dropwizard.metricadapters.HistogramAdapter;
import io.pcp.parfait.dropwizard.metricadapters.MeteredAdapter;
import io.pcp.parfait.dropwizard.metricadapters.TimerAdapter;
import com.google.common.base.Preconditions;

public class MetricAdapterFactoryImpl implements MetricAdapterFactory {

    private MetricDescriptorLookup metricDescriptorLookup;
    private MetricNameTranslator metricNameTranslator;

    public MetricAdapterFactoryImpl(MetricDescriptorLookup metricDescriptorLookup, MetricNameTranslator metricNameTranslator) {
        this.metricDescriptorLookup = metricDescriptorLookup;
        this.metricNameTranslator = metricNameTranslator;
    }

    public MetricAdapterFactoryImpl(MetricDescriptorLookup metricDescriptorLookup) {
        this(metricDescriptorLookup, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetricAdapter createMetricAdapterFor(String originalMetricName, Metric metric) {
        Preconditions.checkArgument(!(metric instanceof MetricSet), "Metric Sets cannot be adapted!!");

        String translatedName = translate(originalMetricName);

        MetricDescriptor descriptor = metricDescriptorLookup.getDescriptorFor(translatedName);

        if (metric instanceof Timer) {
            return new TimerAdapter((Timer) metric, translatedName, descriptor.getDescription());
        }

        if (metric instanceof Histogram) {
            return new HistogramAdapter((Histogram) metric, translatedName, descriptor.getDescription(), descriptor.getUnit());
        }

        if (metric instanceof Counter) {
            return new CountingAdapter((Counter) metric, translatedName, descriptor.getDescription(), descriptor.getSemantics());
        }

        if (metric instanceof Gauge) {
            return new GaugeAdapter<>((Gauge) metric, translatedName, descriptor.getDescription(), descriptor.getUnit(), descriptor.getSemantics());
        }

        if (metric instanceof Metered) {
            return new MeteredAdapter((Metered) metric, translatedName, descriptor.getDescription());
        }

        throw new UnsupportedOperationException(String.format("Unable to produce a monitorable adapter for metrics of class %s (%s)", metric.getClass().getName(), originalMetricName));
    }

    private String translate(String originalName) {
        return metricNameTranslator != null ? metricNameTranslator.translate(originalName) : originalName;
    }
}
