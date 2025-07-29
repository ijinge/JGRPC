package com.ijinge.rpc.provider.service;

import com.ijinge.rpc.provider.service.modal.Goods;

import java.util.List;

public interface GoodsService {
    /**
     * 根据商品id 查询商品
     * @param id
     * @return
     */
    Goods findGoods(Long id);

    List<Goods> GoodsPage();
}
