package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常情况
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if(addressBook == null) {
            //地址簿为空
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.size() == 0) {
            //购物车为空
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);

        //向订单明细表插入多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : list){
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 用户端订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQueryUser(int pageNum, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(pageNum,pageSize);

        //补充订单查询所用的DTO参数
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);

        //获得历史订单信息（缺少明细）
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //填充历史订单明细信息
        List<OrderVO> list = new ArrayList<>();

        if(page != null && page.getTotal() > 0) {
            for(Orders orders : page.getResult()) {
                Long orderId = orders.getId();

                //获取订单明细信息
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 用户端订单详情查询
     * @param id
     * @return
     */
    public OrderVO getById(Long id){
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户端取消订单
     * @param id
     */
    public void cancelById(Long id) throws Exception{
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        //检验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);

        //订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 根据id再来一单
     * @param id
     */
    public void repeatById(Long id){
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);

        Long userId = BaseContext.getCurrentId();
        if(orderDetails != null && orderDetails.size() > 0){
            for(OrderDetail orderDetail : orderDetails){
                ShoppingCart shoppingCart = ShoppingCart.builder()
                        .userId(userId)
                        .name(orderDetail.getName())
                        .number(orderDetail.getNumber())
                        .amount(orderDetail.getAmount())
                        .image(orderDetail.getImage())
                        .createTime(LocalDateTime.now())
                        .build();
                if(orderDetail.getSetmealId() != null){
                    shoppingCart.setSetmealId(orderDetail.getSetmealId());
                }else{
                    shoppingCart.setDishId(orderDetail.getDishId());
                    shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
                }
                shoppingCartMapper.insert(shoppingCart);
            }
        }
    }

    /**
     * 根据条件查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.conditionQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if(page != null && page.getTotal() > 0) {
            for(Orders orders : page.getResult()) {
                OrderVO orderVO = new OrderVO();

                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());

                List<String> dishes = new ArrayList<>();
                if(orderDetails != null && orderDetails.size() > 0){
                    for(OrderDetail orderDetail : orderDetails){
                        if(orderDetail.getDishId() != null){
                            dishes.add(orderDetail.getName());
                        }else {
                            List<SetmealDish> sds = setmealDishMapper.getBySetmealId(orderDetail.getSetmealId());
                            if(sds != null && sds.size() > 0){
                                for(SetmealDish setmealDish : sds){
                                    dishes.add(setmealDish.getName());
                                }
                            }
                        }
                    }
                }

                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDishes(String.valueOf(dishes));

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 统计各个状态订单数量
     * @return
     */
    public OrderStatisticsVO getOrderStatistics() {
        return orderMapper.sum();
    }

    /**
     * 根据id接收订单
     * @param ordersConfirmDTO
     */
    public void confirmById(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .status(Orders.CONFIRMED)
                .id(ordersConfirmDTO.getId())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 根据id拒绝订单
     * @param ordersRejectionDTO
     */
    public void rejectById(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = Orders.builder()
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .id(ordersRejectionDTO.getId())
                .build();

        orderMapper.update(orders);
    }
}
