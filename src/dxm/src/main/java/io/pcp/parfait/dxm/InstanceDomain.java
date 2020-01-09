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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;

class InstanceDomain implements PcpId, PcpOffset, MmvWritable {
    private static final int INSTANCE_DOMAIN_LENGTH = 32;

    private final String name;
    private final int id;
    private int offset;
    private final Store<Instance> instanceStore;
    private PcpString shortHelpText;
    private PcpString longHelpText;

    InstanceDomain(String name, int id, InstanceStoreFactory instanceStoreFactory) {
        this.name = name;
        this.id = id;
        this.instanceStore = instanceStoreFactory.createNewInstanceStore(name, this);
    }

    Instance getInstance(String name) {
    	return instanceStore.byName(name);
    }

    @Override
    public String toString() {
        return name + " (" + id + ") " + instanceStore.all().toString();
    }

    public int getId() {
        return id;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int byteSize() {
        return INSTANCE_DOMAIN_LENGTH;
    }

    private int getInstanceCount() {
        return instanceStore.size();
    }

    private int getFirstInstanceOffset() {
        return instanceStore.all().iterator().next().getOffset();
    }

    Collection<Instance> getInstances() {
        return instanceStore.all();
    }

    void setHelpText(PcpString shortHelpText, PcpString longHelpText) {
        this.shortHelpText = shortHelpText;
        this.longHelpText = longHelpText;
        
    }

    @Override
    public void writeToMmv(ByteBuffer byteBuffer) {
        byteBuffer.position(offset);
        writeInstanceDomainSection(byteBuffer);
        for (Instance instance : getInstances()) {
            instance.writeToMmv(byteBuffer);
        }
    }

    private void writeInstanceDomainSection(ByteBuffer dataFileBuffer) {
        dataFileBuffer.putInt(id);
        dataFileBuffer.putInt(getInstanceCount());
        dataFileBuffer.putLong(getFirstInstanceOffset());
        dataFileBuffer.putLong(getStringOffset(shortHelpText));
        dataFileBuffer.putLong(getStringOffset(longHelpText));
    }


    private long getStringOffset(PcpString text) {
        if (text == null) {
            return 0;
        }
        return text.getOffset();
    }

    static final class InstanceDomainStore extends Store<InstanceDomain> {
        private InstanceStoreFactory instanceStoreFactory;

        InstanceDomainStore(IdentifierSourceSet identifierSources, InstanceStoreFactory instanceStoreFactory) {
            super(identifierSources.instanceDomainSource());
            this.instanceStoreFactory = instanceStoreFactory;
        }

        @Override
        protected InstanceDomain newInstance(String name, Set<Integer> usedIds) {
            return new InstanceDomain(name, identifierSource.calculateId(name, usedIds), instanceStoreFactory);
        }

    }
}