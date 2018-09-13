package com.pinyougou.order.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pinyougou.pojo.TbOrder;

import groupEntity.Orders;

public interface OrderszService {

	List<Orders> findOrderList();

	List<Orders> findOrderBySelectStatus(Map<String, String> orderstat);

	List<Orders> findOrderByDataSelect(Date date, String name);
	

}

