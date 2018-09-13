package com.pinyougou.cart.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;

import groupEntity.Cart;
import groupEntity.CartVo;

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private TbItemMapper itemMapper;

	@Override
	public List<Cart> findCartListFromRedis(String sessionId) {
		List<Cart> list = (List<Cart>) redisTemplate.boundHashOps("cartList").get(sessionId);
		if (list == null) {
			list = new ArrayList<Cart>(0);
		}
		return list;
	}

	@Override
	public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, int num) {

		TbItem tbItem = itemMapper.selectByPrimaryKey(itemId);
		if (tbItem == null) {
			throw new RuntimeException("没有此商品");
		}

		String sellerId = tbItem.getSellerId();
		// 添加购物车时：
		// 1、即将添加的商品对应的商家是否已存在购物车列表中
		Cart cart = findCartBySellerIdFromCartList(cartList, sellerId);
		// 1、1 如果已存在：判断此商品是否已在此商家的购物车对象的orderItemList中
		if (cart != null) {
			List<TbOrderItem> orderItemList = cart.getOrderItemList();
			TbOrderItem orderItem = findOrderItemByItemIdFromOrderItemList(orderItemList, itemId);
			// 1、1、1 如果有直接更新数量
			if (orderItem != null) {
				// 更新数量
				orderItem.setNum(orderItem.getNum() + num);
				// 更新合计金额
				orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));
				if (orderItem.getNum() <= 0) { // 判断数量更新后是否为0
					orderItemList.remove(orderItem);
					if (orderItemList.size() == 0) {
						cartList.remove(cart);
					}
				}

			} else {
				// 1、1、2 如果没有构建一个orderItem对象 把此对象放到orderItemList中
				orderItem = createTbOrderItem(orderItem, tbItem, num);
				orderItemList.add(orderItem);
			}
		} else {
			// 1、2 如果不存在 创建一个cart对象，创建一个orderItemList集合,创建orderItem对象并且放入orderItemList集合，
			cart = new Cart();
			cart.setSellerId(sellerId);
			cart.setSellerName(tbItem.getSeller());
			List<TbOrderItem> orderItemList = new ArrayList<TbOrderItem>();
			TbOrderItem orderItem = null;
			orderItem = createTbOrderItem(orderItem, tbItem, num);
			orderItemList.add(orderItem);
			cart.setOrderItemList(orderItemList);
			// 需要把cart对象放到购物车列表中
			cartList.add(cart);
		}
		return cartList;
	}

	private TbOrderItem createTbOrderItem(TbOrderItem orderItem, TbItem tbItem, int num) {
		if (num < 1) {
			throw new RuntimeException("数量非法");
		}

		orderItem = new TbOrderItem();
		orderItem.setGoodsId(tbItem.getGoodsId());
		orderItem.setItemId(tbItem.getId());
		orderItem.setNum(num);
		orderItem.setPicPath(tbItem.getImage());
		orderItem.setPrice(tbItem.getPrice());
		orderItem.setSellerId(tbItem.getSellerId());
		orderItem.setTitle(tbItem.getTitle());
		orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));
		// orderItemList.add(orderItem);
		return orderItem;
	}

	private TbOrderItem findOrderItemByItemIdFromOrderItemList(List<TbOrderItem> orderItemList, Long itemId) {
		for (TbOrderItem tbOrderItem : orderItemList) {
			if (tbOrderItem.getItemId().longValue() == itemId.longValue()) {
				return tbOrderItem;
			}
		}
		return null;
	}

	public Cart findCartBySellerIdFromCartList(List<Cart> cartList, String sellerId) {
		for (Cart cart : cartList) {
			if (cart.getSellerId().equals(sellerId)) {
				return cart; // 找到了此商品对象的商家的购物车对象
			}
		}
		return null;
	}

	@Override
	public void saveCartListToRedis(String sessionId, List cartList) {
		redisTemplate.boundHashOps("cartList").put(sessionId, cartList);
	}

	@Override
	public List<Cart> mergeCartList(List<Cart> cartList_session, List<Cart> cartList_userId) {
		for (Cart cart : cartList_session) {
			List<TbOrderItem> orderItemList = cart.getOrderItemList();
			for (TbOrderItem tbOrderItem : orderItemList) {
				cartList_userId = addGoodsToCartList(cartList_userId, tbOrderItem.getItemId(), tbOrderItem.getNum());
			}
		}
		return cartList_userId;
	}

	@Override
	public void clearRedisByKey(String key) {
		redisTemplate.boundHashOps("cartList").delete(key);
	}

	@Override
	public List<Cart> addItemToCartList(List<Cart> cartList, Long itemId, Integer num) {
		// 1.根据商品 SKU ID 查询 SKU 商品信息
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		if (item == null) {
			throw new RuntimeException("商品不存在");
		}
		if (!item.getStatus().equals("1")) {
			throw new RuntimeException("商品状态无效");
		}
		// 2.获取商家 ID
		String sellerId = item.getSellerId();

		// 3.根据商家 ID 判断购物车列表中是否存在该商家的购物车
		Cart cart = searchCartBySellerId(cartList, sellerId);
		// 4.如果购物车列表中不存在该商家的购物车
		if (cart == null) {
			// 4.1 新建购物车对象 ，
			cart = new Cart();
			cart.setSellerId(sellerId);
			cart.setSellerName(item.getSeller());
			TbOrderItem orderItem = createOrderItem(item, num);
			List orderItemList = new ArrayList();
			orderItemList.add(orderItem);
			cart.setOrderItemList(orderItemList);
			// 4.2 将购物车对象添加到购物车列表
			cartList.add(cart);
		} else {
			// 5.如果购物车列表中存在该商家的购物车
			// 判断购物车明细列表中是否存在该商品
			TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);
			if (orderItem == null) {
				// 5.1. 如果没有，新增购物车明细
				orderItem = createOrderItem(item, num);
				cart.getOrderItemList().add(orderItem);
			} else {
				// 5.2. 如果有，在原购物车明细上添加数量，更改金额
				orderItem.setNum(orderItem.getNum() + num);
				orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));
				// 如果数量操作后小于等于 0，则移除
				if (orderItem.getNum() <= 0) {
					cart.getOrderItemList().remove(orderItem);// 移除购物车明细
				}
				// 如果移除后 cart 的明细数量为 0，则将 cart 移除
				if (cart.getOrderItemList().size() == 0) {
					cartList.remove(cart);
				}
			}
		}
		return cartList;
	}

	/**
	 * 根据商家 ID 查询购物车对象
	 * 
	 * @param cartList
	 * @param sellerId
	 * @return
	 */
	private Cart searchCartBySellerId(List<Cart> cartList, String sellerId) {
		for (Cart cart : cartList) {
			if (cart.getSellerId().equals(sellerId)) {
				return cart;
			}
		}
		return null;
	}

	/**
	 * 根据商品明细 ID 查询
	 * 
	 * @param orderItemList
	 * @param itemId
	 * @return
	 */
	private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
		for (TbOrderItem orderItem : orderItemList) {
			if (orderItem.getItemId().longValue() == itemId.longValue()) {
				return orderItem;
			}
		}
		return null;
	}

	/**
	 * 创建订单明细
	 * 
	 * @param item
	 * @param num
	 * @return
	 */
	private TbOrderItem createOrderItem(TbItem item, Integer num) {
		
		
		if (num <= 0) {
			throw new RuntimeException("数量非法");

		}
		TbOrderItem orderItem = new TbOrderItem();
		orderItem.setGoodsId(item.getGoodsId());
		orderItem.setItemId(item.getId());
		orderItem.setNum(num);
		orderItem.setPicPath(item.getImage());
		orderItem.setPrice(item.getPrice());
		orderItem.setSellerId(item.getSellerId());
		orderItem.setTitle(item.getTitle());
		orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue() * num));
		return orderItem;
	}

	@Override
	public List<Cart> mergeLocalStrageAndRedis(String userId, CartVo vo) {
		List<Cart> listCart=(List<Cart>) redisTemplate.boundHashOps("cartList").get(userId);
		List<Cart> listLocalStrage=vo.getCartList();
		List<Cart> listReturn=null;
		if(listCart==null) {
			redisTemplate.boundHashOps("cartList").put(userId,vo.getCartList());
		}else {
			listReturn=mergeCartList(listCart,listLocalStrage);
			//先清空redis在放入数据
			redisTemplate.boundHashOps("cartList").delete(userId);
			redisTemplate.boundHashOps("cartList").put(userId,listReturn);
		}
		return listReturn;
	}

}
