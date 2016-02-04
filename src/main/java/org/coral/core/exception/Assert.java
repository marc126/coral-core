package org.coral.core.exception;

import java.util.Collection;

import org.coral.core.utils.UtilString;

public class Assert {
	public static void isTrue(boolean expression,String msg){
		if(!expression)
			throw new BizException(msg);
	}
	
	public static void notNull(Object obj,String msg){
		if(obj==null)
			throw new BizException(msg);
	}
	public static void notNull(Object obj){
		notNull(obj,"Assert notNull exception!");
	}
	
	public static void hasText(String text,String msg){
		if(UtilString.isEmpty(text))
			throw new BizException(msg);
	}
	
	public static void hasText(String text){
		hasText(text,"Assert hasText exception!");
	}

	public static void isTrue(boolean b) {
		isTrue(b,"Assert isTrue exception!");
	}
	
	public static void notEmpty(Collection collection,String msg){
		if(collection==null || collection.isEmpty())
			throw new BizException(msg);
	}
	
	public static void notEmpty(Collection collection){
		notNull(collection,"Assert notEmpty exception!");
	}
}
