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

package com.rebuild.server.metadata;

import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.entityhub.DisplayType;

import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.XmlHelper;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class DynamicMetadataFactory extends ConfigurationMetadataFactory {
	private static final long serialVersionUID = -5709281079615412347L;
	
	private static final Log LOG = LogFactory.getLog(DynamicMetadataFactory.class);
	
	// <Name, [ID, COMMENTS, ICON]>
	private static final Map<String, Object[]> ENTITY_EXTMETA = new CaseInsensitiveMap<>();
	// <Name, [ID, COMMENTS]>
	private static final Map<String, Object[]> FIELD_EXTMETA = new CaseInsensitiveMap<>();
	
	public DynamicMetadataFactory(String configLocation, Dialect dialect) {
		super(configLocation, dialect);
	}
	
	@Override
	protected Document readConfiguration(boolean initState) {
		Document config = super.readConfiguration(initState);
		if (initState == false) {
			appendConfig4Db(config);
		}
		return config;
	}
	
	/**
	 * 从数据库读取配置
	 * 
	 * @param config
	 */
	private void appendConfig4Db(Document config) {
		final Element rootElement = config.getRootElement();
		
		ENTITY_EXTMETA.clear();
		FIELD_EXTMETA.clear();
		
		Object[][] customEntity = Application.createQueryNoFilter(
				"select typeCode,entityName,physicalName,entityLabel,entityId,comments,icon,nameField,masterEntity from MetaEntity order by createdOn")
				.array();
		for (Object[] custom : customEntity) {
			String name = (String) custom[1];
			Element entity = rootElement.addElement("entity");
			entity.addAttribute("type-code", custom[0] + "")
					.addAttribute("name", name)
					.addAttribute("physical-name", (String) custom[2])
					.addAttribute("description", (String) custom[3])
					.addAttribute("parent", "false")
					.addAttribute("name-field", (String) custom[7])
					.addAttribute("master", (String) custom[8]);
			ENTITY_EXTMETA.put(name, new Object[] { custom[4], custom[5], custom[6] });
		}
		
		Object[][] customFields = Application.createQueryNoFilter(
				"select belongEntity,fieldName,physicalName,fieldLabel,displayType,nullable,creatable,updatable,precision,"
				+ "maxLength,defaultValue,refEntity,cascade,fieldId,comments,extConfig from MetaField order by createdOn")
				.array();
		for (Object[] custom : customFields) {
			String entityName = (String) custom[0];
			String fieldName = (String) custom[1];
			Element entityElement = (Element) rootElement.selectSingleNode("entity[@name='" + entityName + "']");
			if (entityElement == null) {
				LOG.warn("无效字段 [ " + entityName + "." + fieldName + " ] 无有效依附实体");
				continue;
			}
			
			Element field = entityElement.addElement("field");
			field.addAttribute("name", fieldName)
					.addAttribute("physical-name", (String) custom[2])
					.addAttribute("description", (String) custom[3])
					.addAttribute("nullable", custom[5].toString())
					.addAttribute("creatable", custom[6].toString())
					.addAttribute("updatable", custom[7].toString())
					.addAttribute("decimal-scale", custom[8].toString())
					.addAttribute("max-length", custom[9].toString())
					.addAttribute("default-value", (String) custom[10])
					.addAttribute("ref-entity", (String) custom[11])
					.addAttribute("cascade", (String) custom[12]);
			if (fieldName.equals(EntityHelper.AutoId)) {
				field.addAttribute("auto-value", "true");
			}
			
			DisplayType dt = DisplayType.valueOf((String) custom[4]);
			field.addAttribute("type", dt.getFieldType().getName());
			
			FIELD_EXTMETA.put(entityName + "." + fieldName, new Object[] { custom[13], custom[14], dt, custom[15] });
		}
		
		if (LOG.isDebugEnabled()) {
			XmlHelper.dump(rootElement);
		}
	}
	
	/**
	 * @param entity
	 * @return
	 */
	protected Object[] getEntityExtmeta(String entity) {
		return ENTITY_EXTMETA.get(entity);
	}
	
	/**
	 * @param entity
	 * @param field
	 * @return
	 */
	protected Object[] getFieldExtmeta(String entity, String field) {
		return FIELD_EXTMETA.get(entity + "." + field);
	}
}
