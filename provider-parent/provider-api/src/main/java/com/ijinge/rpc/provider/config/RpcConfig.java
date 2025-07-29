package com.ijinge.rpc.provider.config;

import com.ijinge.rpc.annotation.EnableRpc;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRpc(serverPort = 13568)
public class RpcConfig {
}
