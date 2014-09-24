package org.test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mvc.annotation.Action;
import org.mvc.annotation.RequestMapping;
import org.mvc.viewModel.ModelMap;
import org.test.model.User;


@Action
@RequestMapping("/user")
public class LoginAction {

	@RequestMapping("/login")
	public String login(HttpServletRequest request,HttpServletResponse response,String token,String username,User user,ModelMap modelMap){
		System.out.println(token);
		modelMap.put("message", "登陆成功");
		modelMap.put("u", user);
		return "/view/success.jsp";
	}
	
	@RequestMapping("/logout")
	public String logout(HttpServletRequest request,HttpServletResponse response){
		return null;
	}
}
