package org.web3j.protocol.eea;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.web3j.protocol.RequestTester;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.eea.Eea;
import org.web3j.protocol.http.HttpService;

public class RequestTest extends RequestTester {
    private Eea web3j;

    @Override
    protected void initWeb3Client(HttpService httpService) {
        web3j = Eea.build(httpService);
    }

    @Test
    public void testEthSendRawTransaction() throws Exception {
        web3j.eeaSendRawTransaction(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f"
                        + "072445675058bb8eb970870f072445675").send();

        //CHECKSTYLE:OFF
        verifyResult("{\"jsonrpc\":\"2.0\",\"method\":\"eea_sendRawTransaction\",\"params\":[\"0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675\"],\"id\":1}");
        //CHECKSTYLE:ON
    }

    @Test
    public void testEeaGetTransactionReceipt() throws Exception {
        web3j.eeaGetTransactionReceipt("0x123").send();

        verifyResult("{\"jsonrpc\":\"2.0\",\"method\":\"eea_getTransactionReceipt\","
                + "\"params\":[\"0x123\"],\"id\":1}");
    }
}
