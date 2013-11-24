package com.eshop.commonsys.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.stereotype.Component;

import com.base.framwork.action.BaseAction;
import com.base.framwork.service.IUtilService;
import com.eshop.domain.CartList;
import com.eshop.domain.UserBuyer;
import com.eshop.model.BuyerAddrModel;
import com.eshop.model.OrderItemModel;
import com.eshop.model.OrderMenuModel;
import com.eshop.model.ProductInfoModel;
import com.eshop.model.UserBuyerModel;
import com.eshop.service.IBuyerAddrService;
import com.eshop.service.IOrderItemService;
import com.eshop.service.IOrderMenuService;
import com.eshop.service.IProductInfoService;
import com.eshop.service.IUserBuyerService;
import com.eshop.util.GetOrderIdUtil;

@Component
public class ShoppingAction extends BaseAction {

	@Resource
	private IBuyerAddrService buyerAddrService;
	
	@Resource
	private IOrderMenuService orderMenuService;
	
	@Resource
	private IOrderItemService orderItemService;
	
	@Resource
	private IProductInfoService productInfoService;
	
	@Resource
	private IUserBuyerService userBuyerService;
	
	@Resource
	private IUtilService utilService;

	//校区
	private String schoolArea;
	
	//具体的地址
	private String building;
	
	//收货人
	private String consignee;
	
	//联系方式
	private String phone;
	
	//备注，更具体的地址、送货时间
	private String address;

	//地址主键
	private String addrId;
	
	private BuyerAddrModel buyerAddr;
	
	public String submitOrder(){
		UserBuyer user = (UserBuyer) doGetSessionObject("loginUser");
		ProductInfoModel productInfoModel = null;
		CartList cartList = (CartList) doGetSessionObject("cartList");
		if(cartList == null){
			return INPUT;
		}
		
		List<OrderItemModel> orderItemModels = cartList.getItems();
	/*	//删除购买数量为0的商品
		for(Iterator<OrderItemModel> iter = orderItemModels.iterator(); iter.hasNext();){
			OrderItemModel item = iter.next();
			if (0 == item.getCount()) {
				iter.remove();
			}
		}*/
		synchronized (orderItemModels) {
			for(OrderItemModel orderItemModel : orderItemModels){
				productInfoModel = productInfoService.findEntityById(orderItemModel.getProductId());
				if(productInfoModel.getRemainNumber() < orderItemModel.getCount())
				{
					addActionMessage("抱歉，"+productInfoModel.getName()+"库存量不足");
					return INPUT;
				}
			}
			for(OrderItemModel orderItemModel : orderItemModels){
				productInfoModel = productInfoService.getEntityById(orderItemModel.getProductId());
				productInfoModel.setRemainNumber(productInfoModel.getRemainNumber() - orderItemModel.getCount());
				productInfoService.updateEntity(productInfoModel, user);
			}
		}

		////收货地址已经存在
		if(null != addrId && !"".equals(addrId)){
			buyerAddr = buyerAddrService.findEntityById(addrId);
			//BuyerAddrModel buyerAddrModel = buyerAddrService.findByUserId(userId)
		}else{
			buyerAddr = new BuyerAddrModel();
		}
		buyerAddr.setSchoolArea(schoolArea);
		buyerAddr.setBuilding(building);
		buyerAddr.setConsignee(consignee);
		buyerAddr.setPhone(phone);
		buyerAddr.setAddress(address);
		//设置为默认地址
		buyerAddr.setIsDefault("1");
		if(null != user){
			buyerAddr.setBuyerId(user.getId());
			buyerAddr.setEmail(user.getEmail());
			buyerAddrService.insertEntity(buyerAddr, user);
			UserBuyerModel userBuyerModel = userBuyerService.findEntityById(user.getId());
			//增加积分
			userBuyerModel.setScore(user.getScore() + (int)cartList.getTotalPrice());
			user.setScore(user.getScore() + (int)cartList.getTotalPrice());
		}else{
			buyerAddrService.insertEntity(buyerAddr, null);
		}
		String orderID = GetOrderIdUtil.nextCode();
		Map<String,List<OrderItemModel>> map = separateOrder(orderItemModels);  //根据不同的卖家将商品分开	
		for(Object o:map.keySet()){
			//分开订单，分别保存
			OrderMenuModel orderMenu = new OrderMenuModel();
			if(null != user){
				//与会员关联
				orderMenu.setBuyerId(user.getId());
			}
			//下单时间
			orderMenu.setOrderdate(utilService.getSystemDateTimeString());
			//订单编号
			orderMenu.setOrderid(orderID);
			orderMenu.setTotalpris(cartList.getTotalPrice());
			orderMenu.setStatus("o");
			
			String pID = map.get(o).get(0).getProductId();
			String shopId = productInfoService.findEntityById(pID).getShopId();
			//与商店关联
			orderMenu.setShopId(shopId);
			//与地址关联
			orderMenu.setAddrId(buyerAddr.getId());
			//保存订单
			orderMenuService.insertEntity(orderMenu, user);

			for(OrderItemModel orderItemModel : map.get(o)){
				//设置订单与订单项的关联
				orderItemModel.setOrderId(orderMenu.getId());
				//设置已完成
				orderItemModel.setIsFinished("1");
				//保存订单内容
				orderItemService.insertEntity(orderItemModel, user);
			}
		}

		ServletActionContext.getContext().put("orderId", orderID);
		ServletActionContext.getContext().put("addressDetail", schoolArea+"  "+building+"  备注： "+address);
		getSession().remove("cartList");
		return SUCCESS;
	}
	
	@SkipValidation
	public String loadAddr(){
		UserBuyer user = (UserBuyer) doGetSessionObject("loginUser");
		if(null != user){
			buyerAddr = buyerAddrService.findByUserId(user.getId());
			if(null != buyerAddr){
				schoolArea = buyerAddr.getSchoolArea();
				building = buyerAddr.getBuilding();
				consignee = buyerAddr.getConsignee();
				phone = buyerAddr.getPhone();
				addrId = buyerAddr.getId();
			}
		}
		return SUCCESS;
	}

	/*
	 * 分开订单
	 * 统计购物车中的商品属于几个不同的商家,然后存进HashMap
	 * 
	 * 遍历List<CartItem>,将得到的current加入myItems
	 * 将current.getSellerId()作为Map的key
	 * 将具有相同的key存到List<>里，然后map.put(key,List<>)
	 * 清空List<>
	 * 进入下轮循环
	 */
	public Map<String,List<OrderItemModel>> separateOrder(List<OrderItemModel> items){
		String sellerId = "";
		List<OrderItemModel> myItems;
		Map<String,List<OrderItemModel>> map = new HashMap<String,List<OrderItemModel>>();
		for(Iterator<OrderItemModel> it = items.iterator(); it.hasNext(); ) 
		{
			OrderItemModel current = it.next();
			myItems = new ArrayList<OrderItemModel>();
			myItems.add(current);
			String productId = current.getProductId();
			ProductInfoModel productInfoModel = productInfoService.findEntityById(productId);
			sellerId = productInfoModel.getShopId();
			for(int i=items.indexOf(current)+1; i<items.size(); i++)
			{
				String pID = items.get(i).getProductId();
				ProductInfoModel pim = productInfoService.findEntityById(pID);
				if(sellerId.equals(pim.getShopId()) ){
					myItems.add(items.get(i));   //加入相同卖家的商品
				}
			}
			if(map.isEmpty() || !map.containsKey(sellerId) )
			{
				map.put(sellerId, myItems);
			}
		}
		return map;
	}
	
	public String getSchoolArea() {
		return schoolArea;
	}

	public void setSchoolArea(String schoolArea) {
		this.schoolArea = schoolArea;
	}

	public String getBuilding() {
		return building;
	}

	public void setBuilding(String building) {
		this.building = building;
	}

	public String getConsignee() {
		return consignee;
	}

	public void setConsignee(String consignee) {
		this.consignee = consignee;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddrId() {
		return addrId;
	}

	public void setAddrId(String addrId) {
		this.addrId = addrId;
	}

	public BuyerAddrModel getBuyerAddr() {
		return buyerAddr;
	}

	public void setBuyerAddr(BuyerAddrModel buyerAddr) {
		this.buyerAddr = buyerAddr;
	}
	
}
