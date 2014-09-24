package org.mvc.action;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mvc.error.ErrorMessage;
import org.mvc.error.ErrorMessage.ErrorType;

import org.mvc.parameter.ActionParameterBean;
import org.mvc.parse.ActionMethodInfo;
import org.mvc.parse.RequestParam;
import org.mvc.viewModel.ModelMap;

/**
 * servlet的总入口，需在web.xml中进行配置
 * @author Administrator
 * 对于action的属性自动注入，现仅支持一级自定义类型属性进行注入   例如： action中有一个person对象，则仅支持到person.username  不支持 person.xxx.xxx
 * 字段类型仅支持 基本类型和string的注入 。
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends HttpServlet{
	
	private static HashSet<String> supportTypes = new HashSet<String>();
	
	private static String errorPage = "view/errorMessage.jsp";
	
	static{
		//初始化所支持的属性
		supportTypes.add("java.lang.String");
		supportTypes.add("java.lang.Boolean");
		supportTypes.add("java.lang.Character");
		supportTypes.add("java.lang.Byte");
		supportTypes.add("java.lang.Integer");
		supportTypes.add("java.lang.Long");
		supportTypes.add("java.lang.Float");
		supportTypes.add("java.lang.Double");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
			//对路径进行处理  去掉前面的“/”  和 后面的“.do”或者“.action”
			String path = this.getActionPath(req.getServletPath());
			//获取解析的action中的method及对应的信息
			Map<String,ActionMethodInfo> actionMethodInfos = (Map<String, ActionMethodInfo>)getServletContext().getAttribute("requestMappings");
			
			//根据请求路径获取方法的相关信息对象——ActionMethodInfo
			ActionMethodInfo cfg = actionMethodInfos.get(path);
			if(cfg==null){
				req.getRequestDispatcher(errorPage).forward(req, resp);
			}
			Object obj = getActionInstanceByMethodInfo(cfg);
			if(obj==null){
				System.out.println("没找到对应的action类："+cfg.getClassFullName());
			}
			try {
				//获取该方法所有的参数信息
				Map<String, ActionParameterBean> fieldMap = cfg.getParameterClass();
				//new一个ModelMap，用于action中的方法在展示数据时向中添加内容。
				ModelMap modelMap = new ModelMap();
				//根据req.getParameterMap() (所有请求的参数) ，参数信息，和cfg.getParamNames()(参数名称)，request，response，modelMap，获取请求的方法所有的对象的List
				Object[] paramObjs = parseRequestParameters(req.getParameterMap(),fieldMap,cfg.getParamNames(),req,resp,modelMap);
				
				String result="";
				try {
					//执行action，并返回结果，根据结果匹配需要返回的url，并进行转发处理
					result = (String) cfg.getActionMethod().invoke(obj,paramObjs);
					
					//获取action中所设置的需要前台展示的内容，并遍历放入request中。
					Map<String,Object> models =  modelMap.getModelMap();
					for (Entry<String,Object> entry : models.entrySet()) {
						req.setAttribute(entry.getKey(), entry.getValue());
					}
					//进行页面跳转
					req.getRequestDispatcher(result).forward(req, resp);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
	}
	
	/**
	 * 根据action的method信息对象，返回对应的action的实例
	 * @param cfg
	 * @return
	 */
	private Object getActionInstanceByMethodInfo(ActionMethodInfo cfg) {
		String classType = cfg.getClassFullName();
		Class clazz = null;
		try {
			clazz = Class.forName(classType);
			if(clazz!=null)
			return clazz.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据request请求的参数信息，method的参数信息，method的参数名称list，及其他参数值，设置method所有参数的值，并返回List
	 * @param parameterMap request.getParameterMap()获取的所有请求参数Map
	 * @param fieldMap method的参数名称：key ，和该参数的相关信息：value （ActionParameterBean此对象定义）
	 * @param paramNameList method的参数名称，并和method的参数顺序一致。
	 * @param req servlet的request 
	 * @param resp servlet的response
	 * @param modelMap 用于action的方法中，向前台展示内容时所需
	 * @return
	 */
	private Object[] parseRequestParameters(Map<String,String[]> parameterMap,
			Map<String, ActionParameterBean> fieldMap, List<String> paramNameList, HttpServletRequest req, HttpServletResponse resp, ModelMap modelMap) {
		//对request中的参数进行循环放入到requestParamMap中，String为参数名称，RequestParam为value，RequestParam中的paramValue为具体的参数值，paramName为它的属性（针对自定义类型，例如user.username）
		Map<String,RequestParam> requestParamMap = new HashMap<String,RequestParam>();
		for (Entry<String,String[]> entry : parameterMap.entrySet()) {
			getRequestParamMap(entry.getKey(),entry.getValue(),requestParamMap);
		}
		
		//按顺序存放方法中的参数的值
		List<Object> paramValueObjs = new ArrayList<Object>();
		
		for (String paramName : paramNameList) {
			//获取请求方法中的参数相关信息
			ActionParameterBean actionParameterBean = fieldMap.get(paramName);
			//获取request中请求的对应参数的值
			RequestParam requestParam = requestParamMap.get(paramName);
			Object obj = null;
			/*
			 * 对某些非提交的参数，直接进行设值
			 * 1.如果为HttpServletRequest类型，则直接注入
			 * 2.如果为HttpServletResponse类型，直接注入
			 * 3.如果为ModelMap （用于前台显示值的对象），则直接注入
			 * 4.如果不为以上内容，则为提交请求的参数，进行解析请求的参数，为对应的对象
			 */
			if(actionParameterBean.getParameterClass() == HttpServletRequest.class){
				obj = req;
			}else if(actionParameterBean.getParameterClass()==HttpServletResponse.class){
				obj = resp;
			}else if(actionParameterBean.getParameterClass()==modelMap.getClass()){
				obj = modelMap;
			}else if(requestParam!=null){
				obj = parseParamToObject(actionParameterBean,requestParam);
			}
			paramValueObjs.add(obj);
		}
		
		//把参数值list类型转为数组类型，并返回
		Object[] paramObjs = new Object[paramValueObjs.size()];
		for (int i = 0; i < paramValueObjs.size(); i++) {
			paramObjs[i]=paramValueObjs.get(i);
		}
		
		return paramObjs;
	}
	
	
	/**
	 * 根据method中 参数的信息 和 对request封装好的 提交的参数及值的信息 返回object参数值
	 * @param actionParameterBean
	 * @param requestParam
	 * @return
	 */
	private Object parseParamToObject(ActionParameterBean actionParameterBean,RequestParam requestParam){
		//获取request提交的参数名称解析成的 参数名称和对应的RequestParam，RequestParam中的paramValue为对应的参数的请求的值
		Map<String,RequestParam> paramValues =  requestParam.getParamName();
		//获取method中此参数的class类型
		Class clazz = actionParameterBean.getParameterClass();
		try {
			//判断如果此类型不是java的基本支持的类型时，且此类型所支持的属性不为空的话，则证明此类型是自定义的类型
			if(!supportTypes.contains(clazz.getName())){
				if(actionParameterBean.getSupportField()!=null){
					
					//new一个此对象的实例
					Object obj = clazz.newInstance();
					
					//获取此参数所支持的属性的Map
					Map<String,ActionParameterBean> methodParam = actionParameterBean.getSupportField();

					//遍历此参数的每个entry，找出每个属性，和对应的参数的相关信息
					for (Entry<String,ActionParameterBean> entry : methodParam.entrySet()) {
						String paramName = entry.getKey();
						ActionParameterBean parameterBean = entry.getValue();
						
						//获取request提交的参数所对应的参数和参数值，并判断如果不为空的话，则递归调用此类，获取参数的值，并最终进行设值
						RequestParam param = paramValues.get(paramName);
						if(param!=null){
							Object paramObj = parseParamToObject(parameterBean,param);
							Field field = parameterBean.getField();
							field.setAccessible(true);
							field.set(obj, paramObj);
						}
						
					}
					//最终返回此参数的对象
					return obj;
				}
				
				//如果值不为空,则返回
			}else if(requestParam.getParamValue()!=null){
				//如果为String数组类型的参数，则返回数组的第一个下标对应的值
				if(requestParam.getParamValue() instanceof String[]){
					return ((String[])requestParam.getParamValue())[0];
				}
				return requestParam.getParamValue();
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	
	/**
	 * 把request中提交的参数值封装成对应的Map 递归调用
	 * @param paramName 参数名称 格式可以是以“.”分隔的  例如：user.username  user.contact.address
	 * @param paramValue 参数值
	 * @param requestParamMap 把参数名称 和对应的自定义value——RequestParam 放入map中   例如 key：password  value(RequestParam.paramValue) password
	 * 如果 为自定义类型 例如 key :user   value 为 RequestParam.paramName , 在paramName中 存放的为对应的key：属性名称 和 RequestParam 为 value
	 * @return
	 */
	private static Map<String,RequestParam> getRequestParamMap(String paramName,String[] paramValue, Map<String, RequestParam> requestParamMap){
		//如果名称中有“.” 则继续递归，如果没有“.”，则直接往requestParamMap中put key：paramName 和 value：RequestParam， value值存放在 RequestParam中的paramValue
		if(paramName.indexOf(".")>0){
			String name = paramName.substring(0, paramName.indexOf("."));
			RequestParam requestParam = requestParamMap.get(name);
			if(requestParam==null){
				requestParam = new RequestParam();
			}
			if(requestParam.getParamName()==null){
				requestParam.setParamName(new HashMap<String, RequestParam>());
			}
			requestParam.setParamName(getRequestParamMap(paramName.substring(paramName.indexOf(".")+1),paramValue,requestParam.getParamName()));
			requestParamMap.put(name, requestParam);
		}else{
			requestParamMap.put(paramName, new RequestParam(paramValue));
		}
		
		return requestParamMap;
	}
	
	

	/**
	 * 根据传入的url的字符串，返回action所需的path
	 * @param url
	 * @return
	 */
	private String getActionPath(String url){
		if(url.indexOf(".")!=-1)
		url = url.substring(1, url.indexOf("."));
		return url;
	}
	
}
