package com.eshop.commonsys.action;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.stereotype.Component;

import com.base.framwork.action.BaseAction;
import com.base.framwork.service.mail.IMailSenderService;
import com.eshop.domain.UserBuyer;
import com.eshop.model.UserBuyerModel;
import com.eshop.service.IUserBuyerService;

/**
 * 用户注册action
 * @author chenas
 *
 */
@Component
public class RegAction extends BaseAction{

	@Resource
	private IUserBuyerService userBuyerService;
	
	@Resource
	private IMailSenderService mailSenderService;
	
	//用于接收页面的参数
	private String name;
	private String email;
	private String password;
	private String password2;
	
	//验证码
	private String securityCode;
	
	@Override
	public String execute() {
		if(null == name || "".equals(name)){
			addFieldError("name", "请输入用户名");
			return INPUT;
		}
		if(null == email || "".equals(email)){
			addFieldError("email", "请输入邮箱地址");
			return INPUT;
		}
		if(null == getPassword() || "".equals(getPassword())){
			addFieldError("password", "请输入密码");
			return INPUT;
		}
		if(!getPassword().equals(getPassword2())){
			addFieldError("password2", "请确认密码");
			return INPUT;
		}
		String serverCode = (String) doGetSessionObject("SESSION_SECURITY_CODE");
		if(securityCode == null || securityCode.equals("") || !serverCode.equals(securityCode)){
			addFieldError(securityCode, "验证码不正确");
			return INPUT;
		}
		if(null != userBuyerService.hasUser(getName(), null)){
			addFieldError("name", "用户名已存在");
			return INPUT;
		}
		if(userBuyerService.hasEmail(email)){
			addFieldError("email", "邮箱已被注册");
			return INPUT;
		}
		UserBuyerModel user = new UserBuyerModel();
		user.setName(getName());
		user.setEmail(email);
		user.setPassword(mdcrypt.MD5(getPassword())); //密码md5加密
		//保存注册信息
		userBuyerService.insertEntity(user, null);
		user = userBuyerService.findEntityById(userBuyerService.hasUser(getName(), null));
		UserBuyer loginUser = new UserBuyer();
		loginUser.setEmail(user.getEmail());
		loginUser.setName(user.getName());
		loginUser.setPassword(user.getPassword());
		doPutSessionObject("loginUser", loginUser);
		sendMail(user);
		addActionMessage("亲，离注册完成还有一步哦，请登录邮箱戳一下链接即可");
		return SUCCESS;  //到首页
	}

	/**
	 * 设置用户名、目的邮箱地址
	 * 邮箱模板
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void sendMail(UserBuyerModel user){
		Map model = new HashMap();
		model.put("username", getName());
		model.put("toMailAddr", email);//http://202.120.223.108/eshop/welcome.action
		model.put("url", "http://www.usstlife.com/eshop/pages/commonsys/mailverify.action?id="+user.getId());
		mailSenderService.setTemplateName("mail-register.vm");
		mailSenderService.sendHtmlWithTemplate(user.getEmail(), model);
	}

	public String getSecurityCode() {
		return securityCode;
	}

	public void setSecurityCode(String securityCode) {
		this.securityCode = securityCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}
	
}
