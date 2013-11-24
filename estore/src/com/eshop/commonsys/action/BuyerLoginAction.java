package com.eshop.commonsys.action;

import java.util.List;

import javax.annotation.Resource;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.stereotype.Component;

import com.base.framwork.action.EntityBaseAction;
import com.eshop.domain.CartList;
import com.eshop.domain.UserBuyer;
import com.eshop.model.OrderItemModel;
import com.eshop.model.UserBuyerModel;
import com.eshop.service.IOrderItemService;
import com.eshop.service.IUserBuyerService;

/**
 * 登录action
 * @author chenas
 *
 */
@Component
public class BuyerLoginAction extends EntityBaseAction<UserBuyerModel> {

	private String name;
	private String password;
	
	@Resource
	private IUserBuyerService userBuyerService;
	
	@Resource
	private IOrderItemService orderItemService;
	
	@Override
	public String execute() throws Exception {
		if(userBuyerService.hasUser(name, password) != null){
			UserBuyerModel user = userBuyerService.findEntityById(userBuyerService.hasUser(name, password));
			UserBuyer loginUser = new UserBuyer();
			loginUser.setId(user.getId());
			loginUser.setEmail(user.getEmail());
			loginUser.setName(user.getName());
			loginUser.setPassword(user.getPassword());
			loginUser.setScore(user.getScore());
		//	loginUser.setPhone(user.getPhone());
		//	loginUser.setRealname(user.getRealname());
			
			List<OrderItemModel> orderItemModels = orderItemService.findOrderItemByUserId(user.getId());
			if(null != orderItemModels && 0<orderItemModels.size()){
				CartList cartList = new CartList();
				cartList.getItems().addAll(orderItemModels);
				doPutSessionObject("cartList", cartList);
			}
			
			doPutSessionObject("loginUser", loginUser); //将用户放进session里面
			return SUCCESS; //到首页
		}else
			addFieldError("name", "用户名或密码不正确");
			getRequest().setAttribute("name", getName());
			return ERROR;
	}

	@SkipValidation
	public String logout(){
		if(doGetSessionObject("loginUser") != null){
			getSession().remove("loginUser");
			if(null != doGetSessionObject("cartList")){
				getSession().remove("cartList");
			}
		}
		return SUCCESS;
	}
	
	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
