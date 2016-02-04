package org.coral.core.web.grid;

import java.util.ArrayList;
import java.util.List;

import org.coral.core.utils.UtilString;

public class GridModelParse {
	public static String[] getAllIndexNames(String model){
		model = UtilString.removeBlank(model);
		List<String> list = new ArrayList<String>();
		while(true){
			int idx = model.indexOf(",index:");
			if(idx==-1) break;
			model=model.substring(idx+8);
			list.add(model.substring(0,model.indexOf(",")-1));
		}
		return list.toArray(new String[]{});
	}

}
