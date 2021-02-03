package org.fisco.bcos.sdk.demo;

import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigInteger;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fisco.bcos.sdk.BcosSDKForProxy;
import org.fisco.bcos.sdk.NetworkHandler.NetworkHandlerHttpsImp;
import org.fisco.bcos.sdk.NetworkHandler.NetworkHandlerImp;
import org.fisco.bcos.sdk.NetworkHandler.model.CertInfo;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.exceptions.ClientException;
import org.fisco.bcos.sdk.client.exceptions.NetworkHandlerException;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.config.model.ProxyConfig;
import org.fisco.bcos.sdk.contract.precompiled.consensus.ConsensusService;
import org.fisco.bcos.sdk.contract.precompiled.sysconfig.SystemConfigService;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.demo.contract.HelloWorld;
import org.fisco.bcos.sdk.log.BcosSDKLogUtil;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.model.NodeVersion;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.fisco.bcos.sdk.transaction.tools.JsonUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // comment on the following line without using logs, only one line
        BcosSDKLogUtil.configLog(Environment.getExternalStorageDirectory().getPath(), Level.TRACE);
        // comment end
        Logger logger = Logger.getLogger(MainActivity.class);

        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                httpRequest(logger);
                                httpsRequest(logger);
                            }
                        })
                .start();
    }

    private void httpRequest(Logger logger) {
        // config param, if you do not pass in the implementation of network access, use the default
        // class
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setChainId("1");
        proxyConfig.setCryptoType(CryptoType.ECDSA_TYPE);
        proxyConfig.setHexPrivateKey(
                "65c70b77051903d7876c63256d9c165cd372ec7df813d0b45869c56fcf5fd564");
        NetworkHandlerImp networkHandlerImp = new NetworkHandlerImp();
        networkHandlerImp.setIpAndPort("http://127.0.0.1:8170/");
        proxyConfig.setNetworkHandler(networkHandlerImp);
        // config param end

        BcosSDKForProxy sdk = BcosSDKForProxy.build(proxyConfig);
        deployAndSendContract(sdk, logger);
        sendPrecompiled(sdk, logger);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            logger.error("exception:", e);
        }
        sdk.stopAll();
    }

    private void httpsRequest(Logger logger) {
        // config param, if you do not pass in the implementation of network access, use the default
        // class
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setChainId("1");
        proxyConfig.setCryptoType(CryptoType.ECDSA_TYPE);
        proxyConfig.setHexPrivateKey(
                "65c70b77051903d7876c63256d9c165cd372ec7df813d0b45869c56fcf5fd564");
        NetworkHandlerHttpsImp networkHandlerImp = new NetworkHandlerHttpsImp();
        networkHandlerImp.setIpAndPort("https://127.0.0.1:8180/");
        CertInfo certInfo = new CertInfo("nginx.crt");
        networkHandlerImp.setCertInfo(certInfo);
        networkHandlerImp.setContext(getApplicationContext());
        proxyConfig.setNetworkHandler(networkHandlerImp);
        // config param end

        BcosSDKForProxy sdk = BcosSDKForProxy.build(proxyConfig);
        deployAndSendContract(sdk, logger);
        sendPrecompiled(sdk, logger);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            logger.error("exception:", e);
        }
        sdk.stopAll();
    }

    private void deployAndSendContract(BcosSDKForProxy sdk, Logger logger) {
        try {
            Client client = sdk.getClient(1);
            NodeVersion nodeVersion = client.getClientNodeVersion();
            logger.info("node version: " + JsonUtils.toJson(nodeVersion));
            HelloWorld sol = HelloWorld.deploy(client, client.getCryptoSuite().getCryptoKeyPair());
            logger.info(
                    "deploy contract , contract address: "
                            + JsonUtils.toJson(sol.getContractAddress()));
            // HelloWorldProxy sol =
            // HelloWorldProxy.load("0x2ffa020155c6c7e388c5e5c9ec7e6d403ec2c2d6", client,
            // client.getCryptoSuite().getCryptoKeyPair());
            TransactionReceipt ret1 = sol.set("Hello, FISCO BCOS.");
            logger.info("send, receipt: " + JsonUtils.toJson(ret1));
            String ret2 = sol.get();
            logger.info("call to return string, result: " + ret2);
            BcosTransaction transaction = client.getTransactionByHash(ret1.getTransactionHash());
            logger.info(
                    "getTransactionByHash, result: " + JsonUtils.toJson(transaction.getResult()));
            BcosTransactionReceipt receipt =
                    client.getTransactionReceipt(ret1.getTransactionHash());
            logger.info("getTransactionReceipt, result: " + JsonUtils.toJson(receipt.getResult()));

            sdk.stopAll();
        } catch (NetworkHandlerException e) {
            logger.error("NetworkHandlerException error info: " + e.getMessage());
        } catch (Exception e) {
            logger.error("error info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendPrecompiled(BcosSDKForProxy sdk, Logger logger) {
        try {
            Client client = sdk.getClient(Integer.valueOf(1));
            CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
            ConsensusService consensusService = new ConsensusService(client, cryptoKeyPair);
            // get the current sealerList and observerList
            List<String> sealerList = client.getSealerList().getResult();
            List<String> observerList = client.getObserverList().getResult();
            logger.debug(
                    "sealerList size: "
                            + sealerList.size()
                            + ", observerList size: "
                            + observerList.size());

            // select a node to operate
            String selectedNode = sealerList.get(0);
            logger.debug("selectedNode: " + selectedNode);

            // add the sealer to the observerList
            consensusService.addObserver(selectedNode);
            sealerList = client.getSealerList().getResult();
            observerList = client.getObserverList().getResult();
            logger.debug(
                    "updated 1, sealerList size: "
                            + sealerList.size()
                            + ", observerList size: "
                            + observerList.size());
            if (!observerList.contains(selectedNode) || sealerList.contains(selectedNode)) {
                logger.error("add the node to the observerList failed");
            }
            // add the node to the sealerList
            consensusService.addSealer(selectedNode);
            sealerList = client.getSealerList().getResult();
            observerList = client.getObserverList().getResult();
            logger.debug(
                    "updated 2, sealerList size: "
                            + sealerList.size()
                            + ", observerList size: "
                            + observerList.size());
            if (observerList.contains(selectedNode) || !sealerList.contains(selectedNode)) {
                logger.error("add the node to the sealerList failed");
            }

            SystemConfigService systemConfigService =
                    new SystemConfigService(client, cryptoKeyPair);
            String key = "tx_count_limit";
            BigInteger value = new BigInteger(client.getSystemConfigByKey(key).getSystemConfig());
            BigInteger updatedValue = value.add(BigInteger.valueOf(1000));
            String updatedValueStr = String.valueOf(updatedValue);
            systemConfigService.setValueByKey(key, updatedValueStr);
            BigInteger queriedValue =
                    new BigInteger(client.getSystemConfigByKey(key).getSystemConfig());
            if (!updatedValue.equals(queriedValue)) {
                logger.error("update system config failed");
            }
        } catch (ClientException | ContractException e) {
            logger.error("exception, error info:" + e.getMessage());
        }
    }
}
