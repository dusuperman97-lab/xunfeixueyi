package com.project.platform.mapper;

import com.project.platform.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PaymentOrderMapper {

    int insert(PaymentOrder entity);

    int updateStatusByOutTradeNo(@Param("outTradeNo") String outTradeNo,
                                  @Param("status") String status,
                                  @Param("tradeNo") String tradeNo);

    @Select("SELECT id, out_trade_no, user_id, points, total_amount, description, status, trade_no, create_time, update_time FROM payment_order WHERE out_trade_no = #{outTradeNo}")
    PaymentOrder selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);
}
