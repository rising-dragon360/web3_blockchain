/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.protocol.core.methods.response.admin;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.annotation.JsonProperty; 
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.Response;

/** admin_nodeInfo. */
public class AdminNodeInfo extends Response<AdminNodeInfo.NodeInfo> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Override
    @JsonDeserialize(using = AdminNodeInfo.ResponseDeserialiser.class)
    public void setResult(NodeInfo result) {
        super.setResult(result);
    }

    public static class NodeInfo {
        private String enode;
        private String id;
        private String ip;
        private String listenAddr;
        private String name;
        private String consensus;
        
        @JsonProperty("protocols")
        @SuppressWarnings("unchecked")
        private void consensusDeserializer(Map<String, Object> protocols) {
            if(protocols.containsKey("istanbul")) {
                consensus = "istanbul";
            } else {
                Map<String, Object> eth = (Map<String, Object>) protocols.get("eth");
                consensus = (String) eth.get("consensus");
            }
        }

        public NodeInfo() {}

        public NodeInfo(
                String enode,
                String id,
                String ip,
                String listenAddr,
                String name,
                String consensus) {
            this.enode = enode;
            this.id = id;
            this.ip = ip;
            this.listenAddr = listenAddr;
            this.name = name;
            this.consensus = consensus;
        }

        public String getEnode() {
            return enode;
        }

        public String getId() {
            return id;
        }

        public String getIp() {
            return ip;
        }

        public String getListenAddr() {
            return listenAddr;
        }

        public String getName() {
            return name;
        }
        
        public String getConsensus() {
            return consensus;
        }
    }

    public static class ResponseDeserialiser extends JsonDeserializer<NodeInfo> {

        private ObjectReader objectReader = ObjectMapperFactory.getObjectReader();

        @Override
        public NodeInfo deserialize(
                JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            if (jsonParser.getCurrentToken() != JsonToken.VALUE_NULL) {
                return objectReader.readValue(jsonParser, NodeInfo.class);
            } else {
                return null; // null is wrapped by Optional in above getter
            }
        }
    }
}
