package org.mvc.parse;

import java.util.Map;


/**
 * 对应的请求信息的参数的值  paramValue为参数的具体值，paramName为该对象的属性（针对自定义对象而进行设计），及其对应的参数值
 * @author Administrator
 *
 */
public class RequestParam {

	private Map<String,RequestParam> paramName;
	
	private String[] paramValue;
	
	

	public RequestParam() {
	}

	public RequestParam(String[] paramValue) {
		this.paramValue = paramValue;
	}

	public Map<String, RequestParam> getParamName() {
		return paramName;
	}

	public void setParamName(Map<String, RequestParam> paramName) {
		this.paramName = paramName;
	}

	public String[] getParamValue() {
		return paramValue;
	}

	public void setParamValue(String[] paramValue) {
		this.paramValue = paramValue;
	}

	
	
}
