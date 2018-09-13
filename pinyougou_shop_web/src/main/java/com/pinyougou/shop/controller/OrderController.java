package com.pinyougou.shop.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderszService;

import groupEntity.Orders;

@RestController
@RequestMapping("/order")
public class OrderController { 
	@Reference
	private OrderszService orderszService;

	@RequestMapping("/findOrderList")
	public List<Orders> findOrderList(){
		
		return orderszService.findOrderList();
	}

	@RequestMapping("/findOrderBySelectStatus")
	public List<Orders> findOrderBySelectStatus(@RequestBody Map<String, String> orderstat) {
		String string = orderstat.get("status");
		String string2 = orderstat.get("date");
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		orderstat.put("name", name);
		List<Orders> list= orderszService.findOrderBySelectStatus(orderstat);
	
		return list;
	}

	@RequestMapping("/findOrderByDataSelect/{date}")
	public List<Orders> findOrderByDataSelect(@PathVariable("date") String date) {
		System.out.println(date);
		Date date2 = null;
		try {
			date2 = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		List<Orders> list = orderszService.findOrderByDataSelect(date2, name);
		return list;
	}

}
