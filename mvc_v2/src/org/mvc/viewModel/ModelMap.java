package org.mvc.viewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于action中，在action的方法中以参数形式传入，可以往此map put值之后，mvc会处理，以支持在页面显示。
 * @author crazy
 *
 */
public class ModelMap {

	private Map<String,Object> modelMap;

	public ModelMap() {
		modelMap = new HashMap<String, Object>();
	}
	
	public void put(String key,Object value){
		modelMap.put(key, value);
	}

	public Map<String, Object> getModelMap() {
		return modelMap;
	}

	public void setModelMap(Map<String, Object> modelMap) {
		this.modelMap = modelMap;
	}
	
	
}
