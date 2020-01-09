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

import static io.pcp.parfait.dxm.InstanceV1.INSTANCE_NAME_LIMIT;
import static io.pcp.parfait.dxm.PcpMetricInfoV1.METRIC_NAME_LIMIT;
import static io.pcp.parfait.dxm.PcpString.STRING_BLOCK_LIMIT;

public enum MmvVersion {
    MMV_VERSION1(1, new MmvVersion1Factory()),
    MMV_VERSION2(2, new MmvVersion2Factory());

    private final int version;
    private final MmvVersionFactory mmvVersionFactory;

    MmvVersion(int version, MmvVersionFactory mmvVersionFactory) {
        this.version = version;
        this.mmvVersionFactory = mmvVersionFactory;
    }

    int getVersion() {
        return version;
    }

    Store<PcpMetricInfo> createMetricInfoStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore) {
        return mmvVersionFactory.createMetricInfoStore(identifierSourceSet, stringStore);
    }

    Store<InstanceDomain> createInstanceDomainStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore) {
        return mmvVersionFactory.createInstanceDomainStore(identifierSourceSet, stringStore);
    }

    MetricNameValidator createMetricNameValidator() {
        return mmvVersionFactory.createMetricNameValidator();
    }

    interface MmvVersionFactory {
        Store<PcpMetricInfo> createMetricInfoStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore);
        Store<InstanceDomain> createInstanceDomainStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore);
        MetricNameValidator createMetricNameValidator();
    }

    private static class MmvVersion1Factory implements MmvVersionFactory {
        @Override
        public Store<PcpMetricInfo> createMetricInfoStore(IdentifierSourceSet identifierSourceSet, PcpStringStore unused) {
            return new PcpMetricInfoV1.MetricInfoStoreV1(identifierSourceSet);
        }

        @Override
        public Store<InstanceDomain> createInstanceDomainStore(IdentifierSourceSet identifierSourceSet, PcpStringStore unused) {
            return new InstanceDomain.InstanceDomainStore(
                    identifierSourceSet, new InstanceStoreFactoryV1(identifierSourceSet)
            );
        }

        @Override
        public MetricNameValidator createMetricNameValidator() {
            return new MetricNameValidator(METRIC_NAME_LIMIT, INSTANCE_NAME_LIMIT);
        }
    }

    private static class MmvVersion2Factory implements MmvVersionFactory {

        @Override
        public Store<PcpMetricInfo> createMetricInfoStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore) {
            return new PcpMetricInfoV2.MetricInfoStoreV2(identifierSourceSet, stringStore);
        }

        @Override
        public Store<InstanceDomain> createInstanceDomainStore(IdentifierSourceSet identifierSourceSet, PcpStringStore stringStore) {
            return new InstanceDomain.InstanceDomainStore(identifierSourceSet,
                    new InstanceStoreFactoryV2(identifierSourceSet, stringStore)
            );
        }

        @Override
        public MetricNameValidator createMetricNameValidator() {
            return new MetricNameValidator(STRING_BLOCK_LIMIT, STRING_BLOCK_LIMIT);
        }
    }
}
