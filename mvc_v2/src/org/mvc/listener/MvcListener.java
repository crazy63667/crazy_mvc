package org.mvc.listener;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mvc.parse.ParseBean;


/**
 * mvc控制器
 * @author crazy
 *
 */
public class MvcListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("程序结束！～～");
	}

	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		String configPath = context.getInitParameter("contextConfigLocation");
		String classPath = Thread.currentThread().getContextClassLoader().getResource("/").getPath();
		try {
			//对地址进行Url解码
			classPath = URLDecoder.decode(classPath,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		/*
		 * 获取web.xml配置的mvc的配置文件路径
		 * 1.如果包含classPath则把classPath替换为对应的绝对路径地址
		 * 2.如果不包含，则使用配置的地址
		 * 3.如果为空，则使用默认的路径
		 */
		if(configPath!=null && configPath.startsWith("classpath:")){
			configPath = configPath.replaceFirst("classpath:", "");
			configPath = classPath + configPath;
		}else if(configPath!=null && !"".equals(configPath)){
			String tomcatPath = context.getRealPath("/");
			configPath = tomcatPath + configPath;
		}else{
			configPath = classPath+"application-mvc.xml";
		}
		//获取Map key（映射url）value（对应方法信息），存入ServletContext中
		context.setAttribute("requestMappings", ParseBean.parse(configPath, classPath));
		
		
	}

}
