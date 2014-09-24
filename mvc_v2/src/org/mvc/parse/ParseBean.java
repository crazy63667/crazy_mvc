package org.mvc.parse;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mvc.annotation.Action;
import org.mvc.annotation.RequestMapping;
import org.mvc.parameter.ActionParameterBean;

/**
 * 用于项目启动时，初始化扫描到的指定路径下的类，对类实现了注解，和方法实现了注解的进行解析成 url为key，ActionMethodInfo为value的map对象
 * @author crazy
 *
 */
public class ParseBean {
	
	
	private static Set<String> basicTypes;
	
	static{
		basicTypes = new HashSet<String>();
		basicTypes.add("java.lang.String");
		basicTypes.add("java.lang.Boolean");
		basicTypes.add("java.lang.Character");
		basicTypes.add("java.lang.Byte");
		basicTypes.add("java.lang.Integer");
		basicTypes.add("java.lang.Long");
		basicTypes.add("java.lang.Float");
		basicTypes.add("java.lang.Double");
	}

	public ParseBean(){}
	
	
	/**
	 * 根据mvc的配置文件路径，和classPath进行解析，并返回方法和对应的映射url的map
	 * @param filePath
	 * @return
	 */
	public static Map<String,ActionMethodInfo> parse(String filePath,String classPath){
		//获取扫描路径
		String scanLocation = getScanPath(filePath);
		String scanClassLocation = scanLocation;
		if(scanLocation!=null){
			scanLocation = classPath + locationHandle(scanLocation);
		}else{
			scanLocation = classPath;
		}
		//获取到对应的需要扫描的路径的File，用于扫描所有.class文件，以支持反射
		File classPathFile = new File(scanLocation);
		//获取逗号隔开的类的全名称
		String classFilePath = getAllClassFileName(classPathFile,scanClassLocation.substring(0, scanClassLocation.lastIndexOf(".")));
		//将以上字符串结果解析成List
		List<String> classFilePathList = handleClassPath(classFilePath);
		//将所有所有的类进行扫描，并对进行相关注解的类，和方法封装成 以访问路径为key，请求方法信息对象为value的map，并返回
		Map<String,ActionMethodInfo> actionMethods = getAllMethodByClassName(classFilePathList);
		return actionMethods;
	}
	
	
	private static Pattern methodPattern = Pattern.compile("\\(((((\\w+/)\\w+)+;)+)\\)");;
	
	/**
	 * 根据传入的list中的类全名称，返回方法信息对象ActionMethodInfo和对应的映射url为key的map
	 * @param classFilePathList
	 * @return
	 */
	private static Map<String, ActionMethodInfo> getAllMethodByClassName(
			List<String> classFilePathList) {
		//用于存放访问地址为key，方法信息为value 的map
		Map<String,ActionMethodInfo> methodMap = new HashMap<String, ActionMethodInfo>();
		//遍历所有类的全名称
		for (String className : classFilePathList) {
			try {
				Class actionClass = Class.forName(className);
				//判断此类是否使用了Action注解，如果不为空，则继续
				Annotation actionAnno = actionClass.getAnnotation(Action.class);
				if(actionAnno!=null){
					String actionMapping="";
					//判断此类是否使用了RequestMapping注解，如果使用了，则获取对应的映射地址
					if(actionClass.isAnnotationPresent(RequestMapping.class)){
						RequestMapping rmAnno = (RequestMapping) actionClass.getAnnotation(RequestMapping.class);
						actionMapping = rmAnno.value();
					}
					
					//获取action中所有方法及对应的参数名称list组成的map
					Map<String, List<String>> paramNamemap = ParseParameterName.getParameNamesByClass(actionClass);
					
					//获取当前类所有方法，并判断是否实现了注解RequestMapping，如果实现了则把此方法的信息添加到map中
					Method[] actionMethods = actionClass.getDeclaredMethods();
					for (Method method : actionMethods) {
						String methodMapping="";
						ActionMethodInfo methodInfo = new ActionMethodInfo();
						if(method.isAnnotationPresent(RequestMapping.class)){
							
							//获取该方法所有的参数类型，并对每个参数进行解析并进行封装，以参数的class类型为key，封装的参数信息为value放入map，最终放入method信息中
							Class[] parameterClass = method.getParameterTypes();
							Map<Class,ActionParameterBean> parameterMap = new HashMap<Class, ActionParameterBean>();
							for (Class pClass : parameterClass) {
								ActionParameterBean parameterBean = getBeanFields(pClass);
								parameterMap.put(pClass, parameterBean);
							}
							Map<String,ActionParameterBean> parameterRealMap = new HashMap<String, ActionParameterBean>();
							
							//遍历action中的方法和参数名称组成的map，把与该方法对应的method找出来，并放入method的信息对象中。
							//需要找出方法名称一致，参数个数一致，以及类型相匹配的参数
							putParameterInfo(paramNamemap, method, parameterClass, methodInfo, parameterRealMap, parameterMap);
							
							//获取方法的注解信息，并拼接成url的路径
							RequestMapping rmAnno = method.getAnnotation(RequestMapping.class);
							methodMapping = actionMapping+rmAnno.value();
							
							//方法的参数信息
							methodInfo.setParameterClass(parameterRealMap);
							//把方法，映射地址，类全名称，放入方法信息里
							if(methodMapping.startsWith("/")){
								methodMapping= methodMapping.substring(1, methodMapping.length());
							}
							methodInfo.setRequestMapping(methodMapping);
							methodInfo.setClassFullName(className);
							methodInfo.setActionMethod(method);
							methodMap.put(methodMapping, methodInfo);
						}
					}
				}else{
					continue;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if(!methodMap.isEmpty()){
			return methodMap;
		}
		return null;
	}
	
	
	/**
	 * 遍历action中的方法和参数名称组成的map，把与该方法对应的method找出来，并放入method的信息对象中。
	 * 需要找出方法名称一致，参数个数一致，以及类型相匹配的参数
	 * @param paramNamemap
	 * @param method
	 * @param parameterClass
	 * @param methodInfo
	 * @param parameterRealMap
	 * @param parameterMap
	 * @throws ClassNotFoundException
	 */
	private static void putParameterInfo(Map<String, List<String>> paramNamemap,Method method,Class[] parameterClass,
			ActionMethodInfo methodInfo,Map<String,ActionParameterBean> parameterRealMap,Map<Class,ActionParameterBean> parameterMap) throws ClassNotFoundException{
		//遍历action中的方法和参数名称组成的map，把与该方法对应的method找出来，并放入method的信息对象中。
		//需要找出方法名称一致，参数个数一致，以及类型相匹配的参数
		for (Entry<String,List<String>> paraName : paramNamemap.entrySet()) {
			//方法名称，以及参数类型
			String methodNameInfo = paraName.getKey();
			//参数的名称
			List<String> methodParaNames = paraName.getValue();
			
			//根据key获得方法名称，如果与此需要封装的方法名称不匹配，则直接进入下次循环
			String methodName = methodNameInfo.substring(0, methodNameInfo.indexOf(","));
			if(!method.getName().equals(methodName)){
				continue;
			}
			//正则匹配方法对应的参数类型
			Matcher methodMatcher = methodPattern.matcher(methodNameInfo);
			while(methodMatcher.find()){
				String paramClasses = methodMatcher.group(1);
				String params[] = paramClasses.split(";");
				//如果参数个数不一致，直接跳出
				if(params.length!=parameterClass.length){
					break;
				}else{
					int sameParamClassTime = 0;
					//如果参数个数一致，则匹配类型是否都一样，如果一样，则把方法中的参数名称的list放入方法信息中
					for (int i = 0;i<params.length;i++) {
						String str = params[i];
						str = str.substring(1).replace("/", ".");
						params[i]=str;
						Class pClass = Class.forName(str);
						if(pClass==parameterClass[i]){
							sameParamClassTime++;
						}
					}
					if(sameParamClassTime==params.length){
						methodInfo.setParamNames(methodParaNames);
					}else{
						break;
					}
					
					//把参数的名称放入对应的参数信息当中
					for (int i = 0; i < params.length; i++) {
						Class pClass = Class.forName(params[i]);
						parameterRealMap.put(methodParaNames.get(i), parameterMap.get(pClass));
					}
					
				}
			}
		}
	}
	
	/**
	 * 传入class类型，返回所有提供set的key(属性名) value（Field）对的属性的map
	 * @param clazz
	 * @return
	 */
	private static ActionParameterBean getBeanFields(Class clazz){
		//对action中的属性进行循环遍历,并以属性名为key，Field为value放入map中
		Field[] fields = clazz.getDeclaredFields();
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		for (Field field : fields) {
			String fieldName = field.getName();
			Class fieldType = field.getType();
			fieldMap.put(fieldName, field);
		}
		
		ActionParameterBean actionParameterBean = new ActionParameterBean();
		actionParameterBean.setParameterClass(clazz);
		//对action中的方法进行遍历，并查找对属性提供set对方法，组成一组key value对的action的form属性
		Map<String,ActionParameterBean> setFields = new HashMap<String, ActionParameterBean>();
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			String name = method.getName();
			Class[] parameterClass = method.getParameterTypes();
			//查找参数的数量为1的
			if(parameterClass.length==1){
				//查找方法是以set开头的
				if(name.startsWith("set")){
					name = name.replaceFirst("set", "");
					//遍历action的属性
					for (Entry<String, Field> entry : fieldMap.entrySet()) {
						String fieldName = entry.getKey();
						//查找属性是和提供的set方法一致的，且类型是一样的
						if(fieldName.equalsIgnoreCase(name)){
							Field field = entry.getValue();
							if(field.getType().getName().equals(parameterClass[0].getName())){
								if(!basicTypes.contains(field.getType())){
									ActionParameterBean parameterBean = getBeanFields(field.getType());
									parameterBean.setField(field);
									setFields.put(fieldName, parameterBean);
								}else{
									setFields.put(fieldName, new ActionParameterBean(field.getType(),field));
								}
							}
						}
					}
				}
			}
		}
		actionParameterBean.setSupportField(setFields);
		
		return actionParameterBean;
	}


	/**
	 * 把string逗号隔开的类全名转换为List类型
	 * @param classFilePath 逗号隔开的java class文件
	 * @param classPath 编译路径
	 * @return
	 */
	private static List<String> handleClassPath(String classFilePath){
		List<String> clasFilePathList = new ArrayList<String>();
		String[] classFilePaths = classFilePath.split(",");
		for (String string : classFilePaths) {
			clasFilePathList.add(string);
		}
		return clasFilePathList;
	}
	
	
	/**
	 * 递归获取传入的文件下的所有class文件的路径，返回逗号隔开的字符串
	 * @param classPathFile
	 * @return
	 */
	private static String getAllClassFileName(File classPathFile,String location){
		if(classPathFile.isDirectory()){
			File[] files = classPathFile.listFiles();
			StringBuffer sb = new StringBuffer();
			for (File file : files) {
				String hereLocation = location;
				hereLocation = hereLocation +"."+classPathFile.getName();
				String classFilePath = getAllClassFileName(file,hereLocation);
				sb.append(classFilePath);
				sb.append(",");
			}
			return sb.substring(0, sb.lastIndexOf(","));
		}else if(classPathFile.getAbsolutePath().endsWith(".class")){
			 return location+"."+classPathFile.getName().replace(".class", "");
		}
		return null;
	}
	
	
	/**
	 * 根据xml配置文件获取扫描路径并返回
	 * @param filePath application-mvc.xml配置文件路径
	 * @return
	 */
	private static String getScanPath(String filePath){
		SAXBuilder builder = new SAXBuilder();
		File strutsXml = new File(filePath);
		if(!strutsXml.exists()){
			System.out.println("路径："+filePath+"的文件不存在！");
			return null;
		}
		Document document=null;
		Element root = null;
		try {
			document = builder.build(strutsXml);
			root = document.getRootElement();
		} catch (JDOMException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if(root!=null){
			Element scan = root.getChild("scan");
			String scanLocation = scan.getAttributeValue("location");
			return scanLocation;
		}
		return null;
	}
	
	/**
	 * 根据传入的字符串，把“.”替换为“/”
	 * @param scanLocation
	 * @return
	 */
	private static String locationHandle(String scanLocation){
		return scanLocation.replaceAll("\\.", "\\"+File.separator);
	}

















}
