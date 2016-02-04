package org.coral.core.web;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.coral.core.exception.BizException;
import org.springframework.util.StringUtils;

public class CustomDateEditor extends PropertyEditorSupport{

	private final Class claz;
	private final boolean allowEmpty;
	
	private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public CustomDateEditor(Class claz, boolean allowEmpty) {
		this.claz = claz;
		this.allowEmpty = allowEmpty;
	}

	/**
	 * Parse the Date from the given text, using the specified DateFormat.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// Treat empty String as null value.
			setValue(null);
		}else {
			Date result = null;
			String regex = "(\\d{4}\\-([0-1]{1})*\\d{1}\\-([0-3]{1})*\\d{1}(\\s([0-2]{1})*\\d{1}\\:\\d{1,2}(\\:\\d{1,2})*)*)";
			try {
				if (!text.matches(regex)) {
					System.out.println("Could not parse date");
					throw new BizException("日期格式错误");
				} else if (text.length() == 10) {
					result = sdf1.parse(text);
				} else if (text.length() == 16) {
					result = sdf2.parse(text);
				} else if (text.length() == 19) {
					result = sdf3.parse(text);
				}
			} catch (ParseException e) {
				throw new BizException("日期格式错误", e);
			}
			setValue(result);
		}
		
	}

	/**
	 * Format the Date as String, using the specified DateFormat.
	 */
	@Override
	public String getAsText() {
		String result = null;
		
		if (getValue() instanceof java.util.Date) {
			java.util.Date from = (java.util.Date)getValue();
			Calendar tempCal = Calendar.getInstance();
			tempCal.setTime(from);
			if(tempCal.get(Calendar.HOUR_OF_DAY)==0&&tempCal.get(Calendar.MINUTE)==0&&tempCal.get(Calendar.SECOND)==0){
				result = sdf1.format(from);
			}else{
				result = sdf2.format(from);
			}
		}
		return result;
	}

}
