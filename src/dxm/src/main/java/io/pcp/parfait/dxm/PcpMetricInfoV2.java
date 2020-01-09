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

package io.pcp.parfait.dxm;

import io.pcp.parfait.dxm.PcpMmvWriter.Store;
import io.pcp.parfait.dxm.PcpString.PcpStringStore;
import io.pcp.parfait.dxm.semantics.UnitMapping;

import java.nio.ByteBuffer;
import java.util.Set;

final class PcpMetricInfoV2 extends PcpMetricInfo {
    private static final int METRIC_LENGTH = 48;
    private PcpString nameAsPcpString;

    PcpMetricInfoV2(String metricName, int id, PcpString nameAsPcpString) {
        super(metricName, id);
        this.nameAsPcpString = nameAsPcpString;
    }

    @Override
    public void writeToMmv(ByteBuffer byteBuffer) {
        byteBuffer.position(offset);

        byteBuffer.putLong(getStringOffset(nameAsPcpString));
        byteBuffer.putInt(getId());
        byteBuffer.putInt(typeHandler.getMetricType().getIdentifier());
        byteBuffer.putInt(getSemantics().getPcpValue());
        byteBuffer.putInt(UnitMapping.getDimensions(getUnit(), metricName));
        if (domain != null) {
            byteBuffer.putInt(domain.getId());
        } else {
            byteBuffer.putInt(DEFAULT_INSTANCE_DOMAIN_ID);
        }
        // Just padding
        byteBuffer.putInt(0);
        byteBuffer.putLong(getStringOffset(shortHelpText));
        byteBuffer.putLong(getStringOffset(longHelpText));
    }

    @Override
    public int byteSize() {
        return METRIC_LENGTH;
    }

    static final class MetricInfoStoreV2 extends Store<PcpMetricInfo> {
        private PcpStringStore stringStore;

        MetricInfoStoreV2(IdentifierSourceSet identifierSources, PcpStringStore stringStore) {
            super(identifierSources.metricSource());
            this.stringStore = stringStore;
        }

        @Override
        protected PcpMetricInfo newInstance(String name, Set<Integer> usedIds) {
            return new PcpMetricInfoV2(name, identifierSource.calculateId(name, usedIds), stringStore.createPcpString(name));
        }
    }
}
