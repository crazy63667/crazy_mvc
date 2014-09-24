package org.mvc.parse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.mvc.parameter.ActionParameterBean;

/**
 * 存放解析的每个action 中 method的信息 
 * @author crazy
 *
 */
public class ActionMethodInfo {
	
	private Method actionMethod;		//action中的method
	
	private String requestMapping;		//method对应的映射地址

	private String classFullName;		//action 类全名称
	
	private Map<String,ActionParameterBean> parameterClass; //方法中的每个参数名称，及其对应的相关信息
	
	private List<String> paramNames;	//该method的方法名称的list，按顺序排列，和方法中参数顺序一致。
	

	public String getClassFullName() {
		return classFullName;
	}

	public void setClassFullName(String classFullName) {
		this.classFullName = classFullName;
	}

	public Method getActionMethod() {
		return actionMethod;
	}

	public void setActionMethod(Method actionMethod) {
		this.actionMethod = actionMethod;
	}

	public String getRequestMapping() {
		return requestMapping;
	}

	public void setRequestMapping(String requestMapping) {
		this.requestMapping = requestMapping;
	}


	public Map<String, ActionParameterBean> getParameterClass() {
		return parameterClass;
	}

	public void setParameterClass(Map<String, ActionParameterBean> parameterClass) {
		this.parameterClass = parameterClass;
	}

	public List<String> getParamNames() {
		return paramNames;
	}

	public void setParamNames(List<String> paramNames) {
		this.paramNames = paramNames;
	}

	

}
