package com.eshop.filter;

import com.base.framwork.queryfilter.QueryFilter;

public class OrderMenuFilter extends QueryFilter {

	//设置每页十个
	@Override
	public int getPageSize() {
		return 10;
	}
	
	

}
