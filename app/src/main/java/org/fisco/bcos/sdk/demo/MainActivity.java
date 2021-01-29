package org.fisco.bcos.sdk.demo;

import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fisco.bcos.sdk.BcosSDKForProxy;
import org.fisco.bcos.sdk.NetworkHandler.NetworkHandlerImp;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.exceptions.NetworkHandlerException;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.config.model.ProxyConfig;
import org.fisco.bcos.sdk.demo.contract.HelloWorld;
import org.fisco.bcos.sdk.log.BcosSDKLogUtil;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.model.NodeVersion;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.tools.JsonUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // comment on the following line without using logs, only one line
                                BcosSDKLogUtil.configLog(
                                        Environment.getExternalStorageDirectory().getPath(),
                                        Level.TRACE);
                                // comment end
                                Logger logger = Logger.getLogger(MainActivity.class);

                                // config param, if you do not pass in the implementation of network
                                // access, use the default class
                                ProxyConfig proxyConfig = new ProxyConfig();
                                proxyConfig.setChainId("1");
                                proxyConfig.setCryptoType(CryptoType.ECDSA_TYPE);
                                proxyConfig.setHexPrivateKey(
                                        "65c70b77051903d7876c63256d9c165cd372ec7df813d0b45869c56fcf5fd564");
                                NetworkHandlerImp networkHandlerImp = new NetworkHandlerImp();
                                networkHandlerImp.setIpAndPort("http://127.0.0.1:8170/");
                                proxyConfig.setNetworkHandler(networkHandlerImp);
                                // config end
                                BcosSDKForProxy sdk = BcosSDKForProxy.build(proxyConfig);
                                try {
                                    Client client = sdk.getClient(1);
                                    NodeVersion nodeVersion = client.getClientNodeVersion();
                                    logger.info("node version: " + JsonUtils.toJson(nodeVersion));
                                    HelloWorld sol =
                                            HelloWorld.deploy(
                                                    client,
                                                    client.getCryptoSuite().getCryptoKeyPair());
                                    logger.info(
                                            "deploy contract , contract address: "
                                                    + JsonUtils.toJson(sol.getContractAddress()));
                                    // HelloWorldProxy sol =
                                    // HelloWorldProxy.load("0x2ffa020155c6c7e388
                                    // c5e5c9ec7e6d403ec2c2d6", client,
                                    // client.getCryptoSuite().getCryptoKeyPair());
                                    TransactionReceipt ret1 = sol.set("Hello, FISCO BCOS.");
                                    logger.info("send, receipt: " + JsonUtils.toJson(ret1));
                                    String ret2 = sol.get();
                                    logger.info("call to return string, result: " + ret2);
                                    BcosTransaction transaction =
                                            client.getTransactionByHash(ret1.getTransactionHash());
                                    logger.info(
                                            "getTransactionByHash, result: "
                                                    + JsonUtils.toJson(transaction.getResult()));
                                    BcosTransactionReceipt receipt =
                                            client.getTransactionReceipt(ret1.getTransactionHash());
                                    logger.info(
                                            "getTransactionReceipt, result: "
                                                    + JsonUtils.toJson(receipt.getResult()));
                                } catch (NetworkHandlerException e) {
                                    logger.error(
                                            "NetworkHandlerException error info: "
                                                    + e.getMessage());
                                } catch (Exception e) {
                                    logger.error("error info: " + e.getMessage());
                                    e.printStackTrace();
                                }

                                try {
                                    Thread.sleep(3000);
                                } catch (Exception e) {
                                    logger.error("exception:", e);
                                }
                                sdk.stopAll();
                            }
                        })
                .start();
    }
}
