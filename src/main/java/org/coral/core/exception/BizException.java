package org.coral.core.exception;


/**
 * 业务异常基类.
 *
 */
@SuppressWarnings("serial")
public class BizException extends RuntimeException {

	/**
	 * 
	 */
	private String messageKey;
	
	public BizException() {
	}

	public BizException(String message) {
		super(message);
		this.messageKey = message;
	}

	public BizException(String message, Throwable cause) {
		super(message, cause);
		this.messageKey = message;
	}

	public BizException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * 返回业务异常信息key
	 * 用于业务代码通过key值来取得业务异常描述
	 * @return
	 */
	public String getMessageKey() {
		return messageKey;
	}	
}
