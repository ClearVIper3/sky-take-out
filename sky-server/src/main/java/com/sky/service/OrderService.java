package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);


    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQueryUser(int pageNum, int pageSize, Integer status);

    /**
     * 用户端订单详情查询
     * @param id
     * @return
     */
    OrderVO getById(Long id);

    /**
     * 用户端取消订单
     * @param id
     */
    void cancelById(Long id) throws Exception;

    /**
     * 根据id再来一单
     * @param id
     */
    void repeatById(Long id);

    /**
     * 根据条件查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各个状态订单数量
     * @return
     */
    OrderStatisticsVO getOrderStatistics();

    /**
     * 根据id接收订单
     * @param ordersConfirmDTO
     */
    void confirmById(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 根据id拒绝订单
     * @param ordersRejectionDTO
     */
    void rejectById(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 商家端取消订单
     * @param ordersCancelDTO
     */
    void cancelByIdAdmin(OrdersCancelDTO ordersCancelDTO);

    /**
     * 根据id派送订单
     * @param id
     */
    void deliveryById(Long id);

    /**
     * 根据id完成订单
     * @param id
     */
    void completeById(Long id);
}
