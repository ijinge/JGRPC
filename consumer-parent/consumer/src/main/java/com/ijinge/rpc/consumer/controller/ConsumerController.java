package com.ijinge.rpc.consumer.controller;

import com.ijinge.rpc.annotation.IjingeReference;
import com.ijinge.rpc.annotation.IjingeService;
import com.ijinge.rpc.provider.service.GoodsService;
import com.ijinge.rpc.provider.service.modal.Goods;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("consumer")
public class ConsumerController {
//    @Autowired
//    private GoodsHttpRpc goodsHttpRpc;
//    @IjingeReference(host = "localhost", port = 13567)

    @IjingeReference
    private GoodsService goodsService;


    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsService.findGoods(id);
    }

    @GetMapping("/goodsPage")
    public List<Goods> GoodsPage(){
        return goodsService.GoodsPage();
    }
}
