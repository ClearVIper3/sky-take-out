package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 获得营业额统计
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //计算出来中间每天的日期
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin);

        List<Double> turnoverList = new ArrayList<>();
        while(!begin.equals(end)) {
            //获取每天的营业额: 状态为“已完成”的订单金额总和
            Map map = new HashMap();
            map.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
            map.put("end", LocalDateTime.of(begin, LocalTime.MAX));
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            //把获得的营业额加入到每天营业额的集合里
            turnoverList.add(turnover);

            //日期计算，每次都跳下一天
            begin = begin.plusDays(1);
            //把计算出来的日期加入每天日期的集合里
            dateList.add(begin);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 获得用户统计
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //计算出来中间每天的日期
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin);

        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        while(!begin.equals(end)) {

            Map map = new HashMap();
            map.put("end", LocalDateTime.of(begin, LocalTime.MAX));
            //统计当日总用户量
            totalUserList.add(userMapper.countByMap(map));
            map.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
            //统计当日新用户量
            newUserList.add(userMapper.countByMap(map));

            //日期计算，每次都跳下一天
            begin = begin.plusDays(1);
            //把计算出来的日期加入每天日期的集合里
            dateList.add(begin);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }
}
