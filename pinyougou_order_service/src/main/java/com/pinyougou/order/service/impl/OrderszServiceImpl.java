package com.pinyougou.order.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.order.service.OrderszService;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderExample;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbOrderItemExample;

import groupEntity.Orders;
import util.DateUtils;
@Service
public class OrderszServiceImpl implements OrderszService {
	@Autowired
	private TbOrderMapper orderMapper;
	@Autowired
	private TbOrderItemMapper orderItemMapper;
	@Autowired
	private TbGoodsMapper goodsMapper;
	

	@Override
	public List<Orders> findOrderList() {
		List<Orders> list= new ArrayList<>();
		
		List<TbOrder> tbOrders = orderMapper.selectByExample(null);
		for (TbOrder tbOrder : tbOrders) {
			Long orderId = tbOrder.getOrderId();
			TbOrderItemExample example=new TbOrderItemExample();
			example.createCriteria().andOrderIdEqualTo(orderId);
			 List<TbOrderItem>  orderItems = orderItemMapper.selectByExample(example);
			 Long goodsId = orderItems.get(0).getGoodsId();
			 TbGoods tbGoods = goodsMapper.selectByPrimaryKey(goodsId);
			 
			 
			 Orders orders= new Orders();
			 orders.setTbGoods(tbGoods);
			orders.setTbOrder(tbOrder);
			orders.setTbOrderItem(orderItems.get(0));
			list.add(orders);
		}
				
		return list;		
	}


	@Override
	public List<Orders> findOrderBySelectStatus(Map<String, String> orderstat) {
		
//		List<Orders> ordersList=new ArrayList<>();
		
		//日订单查询
		if(orderstat.get("date").equals("1")) {
			TbOrderExample example=new TbOrderExample();
			Calendar ca = Calendar.getInstance();//得到一个Calendar的实例 
			ca.setTime(new Date()); //设置时间为当前时间 
			ca.add(Calendar.DATE, -1); //年份减1 
			Date value1 = ca.getTime(); //结果
				Date value2 = new Date();
			if (orderstat.get("status").equals("0")) {
				example.createCriteria().andCreateTimeBetween(value1, value2).andSellerIdEqualTo(orderstat.get("name"));
				}else {
					
					example.createCriteria().andCreateTimeBetween(value1, value2).andStatusEqualTo(orderstat.get("status")).andSellerIdEqualTo(orderstat.get("name"));
				}
				
				List<TbOrder> tbOrders = orderMapper.selectByExample(example);
				List<Orders> byTbOrder = findOrdersByTbOrder(tbOrders);
				return byTbOrder;
			
		}
		//周订单
		if(orderstat.get("date").equals("2")) {
			
			TbOrderExample example=new TbOrderExample();
			//获取当前周的起止时间
			Date[] dates = DateUtils.getWeekStartAndEndDate(new Date());
			Date value1 = dates[0];
			Date value2 = dates[1];
			if (orderstat.get("status").equals("0")) {
				example.createCriteria().andCreateTimeBetween(value1, value2).andSellerIdEqualTo(orderstat.get("name"));
				}else {
					
					example.createCriteria().andCreateTimeBetween(value1, value2).andStatusEqualTo(orderstat.get("status")).andSellerIdEqualTo(orderstat.get("name"));
				}
				
				List<TbOrder> tbOrders = orderMapper.selectByExample(example);
				List<Orders> byTbOrder = findOrdersByTbOrder(tbOrders);
				return byTbOrder;
					
		}
		//月订单
		if(orderstat.get("date").equals("3")) {
			TbOrderExample example=new TbOrderExample();
			//获取当前月的起止时间
			Date[] dates = DateUtils.getMonthStartAndEndDate(new Date());
			Date value1 = dates[0];
			Date value2 = dates[1];
			if (orderstat.get("status").equals("0")) {
			example.createCriteria().andCreateTimeBetween(value1, value2).andSellerIdEqualTo(orderstat.get("name"));
			}else {
				
				example.createCriteria().andCreateTimeBetween(value1, value2).andStatusEqualTo(orderstat.get("status")).andSellerIdEqualTo(orderstat.get("name"));
			}
			
			List<TbOrder> tbOrders = orderMapper.selectByExample(example);
			List<Orders> byTbOrder = findOrdersByTbOrder(tbOrders);
			return byTbOrder;
		}
		
		else {
			System.out.println("进入了else");
			return findOrderList();
		}
		
		
	}
	//根据TbOrder  得到组合类Orders
	
	public List<Orders> findOrdersByTbOrder(List<TbOrder> tbOrders) {
		List<Orders> list= new ArrayList<>();
		
		for (TbOrder tbOrder : tbOrders) {
			Long orderId = tbOrder.getOrderId();
			TbOrderItemExample example=new TbOrderItemExample();
			example.createCriteria().andOrderIdEqualTo(orderId);
			 List<TbOrderItem>  orderItems = orderItemMapper.selectByExample(example);
			 Long goodsId = orderItems.get(0).getGoodsId();
			 TbGoods tbGoods = goodsMapper.selectByPrimaryKey(goodsId);
			 
			 
			 Orders orders= new Orders();
			 orders.setTbGoods(tbGoods);
			orders.setTbOrder(tbOrder);
			orders.setTbOrderItem(orderItems.get(0));
			list.add(orders);
		}
				
		return list;		
	}



	public List<Orders> findOrderByDataSelect(Date date,String name) {
		
		String[] startAndEndTimePointStr = DateUtils.getDayStartAndEndTimePointStr(date);
		String startDate = startAndEndTimePointStr[0];
		String endDate = startAndEndTimePointStr[1];
		System.out.println(endDate);
		try {
			Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startDate);
			Date date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endDate);
			TbOrderExample example=new TbOrderExample();
			example.createCriteria().andCreateTimeBetween(date1, date2).andSellerIdEqualTo(name);
			List<TbOrder> selectByExample = orderMapper.selectByExample(example);
			List<Orders> ordersByDate = findOrdersByTbOrder(selectByExample);
			return ordersByDate;
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;
		}
	}

}
