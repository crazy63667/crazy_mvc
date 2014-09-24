package org.mvc.error;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessage {

	private String message;
	
	private boolean isError=false;
	
	private static Map<Enum,String> errorInfo = new HashMap<Enum, String>();
	
	static{
		errorInfo.put(ErrorType.BooleanFormat, "参数类型错误，boolean类型只能为true或者false");
		errorInfo.put(ErrorType.ByteFormat, "参数类型错误，byte类型转换异常");
		errorInfo.put(ErrorType.CharacterFormat, "参数类型错误，char类型长度只能为1");
		errorInfo.put(ErrorType.DoubleFormat, "参数类型错误，double类型的格式为数字 xxx.xx，如1234.56");
		errorInfo.put(ErrorType.FloatFormat, "参数类型错误，float类型只能为true或者false");
		errorInfo.put(ErrorType.IntegerFormat, "参数类型错误，Integer类型错误");
		errorInfo.put(ErrorType.LongFormat, "参数类型错误，Long类型错误");
	}

	public String getMessage() {
		return message;
	}

	public boolean isError() {
		return isError;
	}

	
	public void setError(ErrorType errorType){
		this.message = errorInfo.get(errorType);
		this.isError = true;
	}
	
	
	public enum ErrorType{
		IntegerFormat(1),DoubleFormat(2),FloatFormat(3),LongFormat(4),BooleanFormat(5),CharacterFormat(6),ByteFormat(7);
		private int state;
		private ErrorType(int statea){
			this.state = statea;
		}
	}
	
	
}
