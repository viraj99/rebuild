/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.rebuild.utils.RbDateCodec;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 响应前端/外部请求
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class Controll {

	static {
		// for fastjson Serialize
		SerializeConfig.getGlobalInstance().put(ID.class, ToStringSerializer.instance);
		SerializeConfig.getGlobalInstance().put(Date.class, RbDateCodec.instance);
	}
	
	/**
	 * OK
	 */
	public static final int CODE_OK = 0;
	/**
	 * 业务错误
	 */
	public static final int CODE_FAIL = 600;
	/**
	 * 系统异常
	 */
	public static final int CODE_ERROR = 999;
	
	protected static Log LOG = LogFactory.getLog(BaseControll.class);
	
	/**
	 * @param data
	 * @return
	 */
	protected JSONObject formatSuccess(Object data) {
		JSONObject map = new JSONObject();
		map.put("error_code", CODE_OK);
		map.put("error_msg", "调用成功");
		if (data != null) {
			if (Record.class.isAssignableFrom(data.getClass())) {
				Record record = (Record) data;
				Map<String, Object> recordMap = new HashMap<String, Object>();
				for (Iterator<String> iter = ((Record) data).getAvailableFieldIterator(); iter.hasNext(); ) {
					String field = iter.next();
					recordMap.put(field, record.getObjectValue(field));
				}
				data = recordMap;
			}
			map.put("data", data);
		}
		return map;
	}
	
	/**
	 * @param errorMsg
	 * @return
	 */
	protected JSONObject formatFailure(String errorMsg) {
		return formatFailure(CODE_ERROR, errorMsg);
	}
	
	/**
	 * @param errorCode
	 * @param errorMsg
	 * @return
	 */
	protected JSONObject formatFailure(int errorCode, String errorMsg) {
		JSONObject map = new JSONObject();
		map.put("error_code", errorCode);
		map.put("error_msg", StringUtils.defaultIfBlank(errorMsg, "系統繁忙，请稍后重试"));
		return map;
	}
}
