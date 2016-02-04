package org.coral.core.web;

import org.coral.core.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseEntityController<S extends BaseService> extends BaseController {

	@Autowired
	protected S baseService; // Action管理Entity所用的manager.


}
