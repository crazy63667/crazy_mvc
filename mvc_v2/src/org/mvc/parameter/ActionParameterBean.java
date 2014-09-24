package org.mvc.parameter;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * method中的参数的类型class，Method，及自定义类型中的属性
 * @author Administrator
 *
 */
public class ActionParameterBean {

	private Class parameterClass;
		
	private Field field;
	
	private Map<String, ActionParameterBean> supportField;	//该属性类型的所有支持的属性 (针对自定义类型，例如user类型的username属性)
	
	public ActionParameterBean(){
	}
	
	public ActionParameterBean(Class parameterClass,Field field){
		this.parameterClass = parameterClass;
		this.field = field;
	}
	
	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public Map<String, ActionParameterBean> getSupportField() {
		return supportField;
	}

	public void setSupportField(Map<String, ActionParameterBean> supportField) {
		this.supportField = supportField;
	}

	public Class getParameterClass() {
		return parameterClass;
	}

	public void setParameterClass(Class parameterClass) {
		this.parameterClass = parameterClass;
	}

	
}
