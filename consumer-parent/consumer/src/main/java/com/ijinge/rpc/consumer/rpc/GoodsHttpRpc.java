package com.ijinge.rpc.consumer.rpc;


import com.ijinge.rpc.annotation.IjingeHttpClient;
import com.ijinge.rpc.annotation.IjingeMapping;
import com.ijinge.rpc.provider.service.modal.Goods;
import org.springframework.web.bind.annotation.PathVariable;

@IjingeHttpClient(value = "goodsHttpRpc")
public interface GoodsHttpRpc {

    @IjingeMapping(url = "http://localhost:7777",api = "/provider/goods/{id}")
    Goods findGoods(@PathVariable Long id);
}
