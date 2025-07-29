package com.ijinge.rpc.provider.service.impl;

import com.ijinge.rpc.annotation.IjingeReference;
import com.ijinge.rpc.annotation.IjingeService;
import com.ijinge.rpc.provider.service.GoodsService;
import com.ijinge.rpc.provider.service.modal.Goods;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@IjingeService(version = "1.0")
// 发布服务，消费方才能发现这个服务
@Service
public class GoodsServiceImpl implements GoodsService {

    public Goods findGoods(Long id) {
        return new Goods(id,"服务提供方商品", BigDecimal.valueOf(100));
    }

    public List<Goods> GoodsPage() {
        ArrayList<Goods> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Goods((long)i,"随机商品",BigDecimal.valueOf(10)));
        }
        return list;
    }
}
