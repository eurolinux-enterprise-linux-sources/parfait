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

package io.pcp.parfait.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

class CPUThreadTestRunner implements Runnable {
    private final int iterations;
    private final CpuLookupMethod cpuLookupMethod;
    private BlockedMetricCollector blockedMetricCollector;

    static enum CpuLookupMethod {
        USE_CURRENT_THREAD_CPU_TIME, USE_CURRENT_THREAD_ID, USE_THREAD_INFO
    }
    
    public CPUThreadTestRunner(int iterations, CpuLookupMethod cpuLookupMethod) {
        this.iterations = iterations;
        this.cpuLookupMethod = cpuLookupMethod;
    }

    @Override
    public void run() {
        this.blockedMetricCollector = new BlockedMetricCollector();

        for (int i = 0; i < iterations; i++) {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            accessThreadCpuInformation(threadMXBean);
        }
        blockedMetricCollector.computeFinalValues();
    }

    private void accessThreadCpuInformation(ThreadMXBean threadMXBean) {
        switch (cpuLookupMethod) {
            case USE_CURRENT_THREAD_ID:
                threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
                break;
            case USE_CURRENT_THREAD_CPU_TIME:
                threadMXBean.getCurrentThreadCpuTime();
                break;
            case USE_THREAD_INFO:
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(Thread.currentThread().getId());
                threadMXBean.getThreadCpuTime(threadInfo.getThreadId());
                break;
            default:
                throw new IllegalStateException("Non-valid CpuLookupMethod: " + cpuLookupMethod);

        }
    }

    public BlockedMetricCollector getBlockedMetricCollector() {
        return blockedMetricCollector;
    }
}