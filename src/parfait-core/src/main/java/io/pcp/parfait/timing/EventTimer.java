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

package io.pcp.parfait.timing;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static tec.uom.se.AbstractUnit.ONE;
import javax.measure.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pcp.parfait.MonitorableRegistry;
import io.pcp.parfait.MonitoredCounter;
import com.google.common.collect.ImmutableList;
import net.jcip.annotations.ThreadSafe;

/**
 * A class to provide a {@link EventMetricCollector} to each {@link Timeable} on demand, guaranteed
 * to be thread-safe as long is it's only ever used by the requesting thread.
 */
@ThreadSafe
public class EventTimer {

    /**
     * Setting this {@link Logger} to DEBUG level will list all the created metrics in a
     * tab-delimited format, useful for importing elsewhere
     */
    private static final Logger LOG = LoggerFactory.getLogger(EventTimer.class);

    private final Map<Object, EventCounters> perEventGroupCounters = new ConcurrentHashMap<Object, EventCounters>();

    private final List<StepMeasurementSink> stepMeasurementSinks;

    private final ThreadValue<EventMetricCollector> metricCollectors = new ThreadValue.WeakReferenceThreadMap<EventMetricCollector>() {
        @Override
        protected EventMetricCollector initialValue() {
            return new EventMetricCollector(perEventGroupCounters, stepMeasurementSinks);
        }
    };


    /**
     * Holds the singleton total counters which are used across all events. The key is the
     * metric name
     */
    private final Map<String, MonitoredCounter> totalCountersAcrossEvents = new HashMap<String, MonitoredCounter>();

    private final ThreadMetricSuite metricSuite;
    private final String prefix;
    private final MonitorableRegistry registry;

    public EventTimer(String prefix, MonitorableRegistry registry, ThreadMetricSuite metrics,
                      boolean enableCpuCollection, boolean enableContentionCollection) {
        this(prefix, registry, metrics, enableCpuCollection, enableContentionCollection, Collections.<StepMeasurementSink>emptyList());
    }

    public EventTimer(String prefix, MonitorableRegistry registry, ThreadMetricSuite metrics,
                      boolean enableCpuCollection, boolean enableContentionCollection, List<StepMeasurementSink> stepMeasurementSinks) {
        this.metricSuite = metrics;
        this.prefix = prefix;
        this.registry = registry;
        if (enableCpuCollection) {
            ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
        }
        if (enableContentionCollection) {
            ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
        }
        this.stepMeasurementSinks = ImmutableList.copyOf(stepMeasurementSinks);
    }

    public EventMetricCollector getCollector() {
        return metricCollectors.get();
    }

    public void registerTimeable(Timeable timeable, String eventGroup) {
        timeable.setEventTimer(this);
        perEventGroupCounters.put(timeable, getCounterSet(eventGroup));
    }

    public void registerMetric(String eventGroup) {
        perEventGroupCounters.put(eventGroup, getCounterSet(eventGroup));
    }

    private EventCounters getCounterSet(String eventGroup) {
        EventMetricCounters invocationCounter = createEventMetricCounters(eventGroup, "count",
                "Total number of times the event was directly triggered", ONE);
        EventCounters counters = new EventCounters(invocationCounter, cleanName(eventGroup));

        for (ThreadMetric metric : metricSuite.metrics()) {
            EventMetricCounters timingCounter = createEventMetricCounters(eventGroup, metric
                    .getCounterSuffix(), metric.getDescription(), metric.getUnit());
            counters.addMetric(metric, timingCounter);
        }

        return counters;
    }

    private MonitoredCounter createMetric(String beanName, String metric, String description, Unit<?> unit) {
        String metricName = getMetricName(beanName, metric);
        String metricDescription = String.format(description, beanName);
        LOG.debug("Created metric: " + metricName + "\t" + metricDescription);
        return new MonitoredCounter(metricName, metricDescription, registry, unit);
    }

    private EventMetricCounters createEventMetricCounters(String beanName, String metric,
                                                          String metricDescription, Unit<?> unit) {
        MonitoredCounter metricCounter = createMetric(beanName, metric, metricDescription + " ["
                + beanName + "]", unit);
        MonitoredCounter totalCounter;

        totalCounter = totalCountersAcrossEvents.get(metric);
        if (totalCounter == null) {
            totalCounter = new MonitoredCounter(getTotalMetricName(metric), metricDescription
                    + " [TOTAL]", registry, unit);
            totalCountersAcrossEvents.put(metric, totalCounter);
        }

        return new EventMetricCounters(metricCounter, totalCounter);
    }

    private String getMetricName(String eventGroup, String metric) {
        return prefix + "." + cleanName(eventGroup) + "." + metric;
    }

    private String cleanName(String eventGroup) {
        // TODO do name cleanup elsewhere
        return eventGroup.replace("/", "");
    }

    private String getTotalMetricName(String metric) {
        return prefix + ".total." + metric;
    }

    Integer getNumberOfTotalEventCounters() {
        return totalCountersAcrossEvents.size();
    }

    EventCounters getCounterSetForEventGroup(Object eventGroup) {
        return perEventGroupCounters.get(eventGroup);
    }

    ThreadMetricSuite getMetricSuite() {
        return metricSuite;
    }

    Map<Thread, EventMetricCollector> getCollectorThreadMap() {
        return metricCollectors.asMap();
    }
}
