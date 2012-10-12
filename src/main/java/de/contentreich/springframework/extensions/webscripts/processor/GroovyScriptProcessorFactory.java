/*
 * deas - http://www.contentreich.de
 *
 * Created on Apr 13, 2012
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.contentreich.springframework.extensions.webscripts.processor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.ScriptProcessor;
import org.springframework.extensions.webscripts.ScriptProcessorFactory;

public class GroovyScriptProcessorFactory implements ScriptProcessorFactory, ApplicationContextAware, InitializingBean {
	protected ApplicationContext applicationContext = null;
	private static final Log log = LogFactory
			.getLog(GroovyScriptProcessorFactory.class);

	private Object ripOffRepoScriptProcessor = null;
	private Map<String, Object> otherProcessorExtensions;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}

	public void setRipOffRepoScriptProcessor(Object ripOffRepoScriptProcessor) {
		this.ripOffRepoScriptProcessor = ripOffRepoScriptProcessor;
	}

	@Override
	public ScriptProcessor newInstance() {
		return new GroovyScriptProcessor(otherProcessorExtensions);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.ripOffRepoScriptProcessor != null) {
			this.otherProcessorExtensions = new HashMap<String, Object>();
			log.debug("Ripping off a "
					+ this.ripOffRepoScriptProcessor.getClass().getName());
			try {
				Method gpe = this.ripOffRepoScriptProcessor.getClass()
						.getMethod("getProcessorExtensions", null);
				Collection<Object> ripOffExtensions = (Collection<Object>) gpe
						.invoke(this.ripOffRepoScriptProcessor, null);
				if (ripOffExtensions != null) {
					for (Iterator iterator = ripOffExtensions.iterator(); iterator
							.hasNext();) {
						Object pe = iterator.next();
						Method gxn = pe.getClass().getMethod("getExtensionName", null);
						String name = (String) gxn.invoke(pe, null);
						this.otherProcessorExtensions.put(name, pe);
					}
					log.debug("Ripped off " + ripOffExtensions.size()
							+ " alien extensions");
				} else {
					log.debug("Got null value for expected property processorExtensions");
				}
			} catch (Exception e) {
				log.warn("Ugh : " + e.getMessage());
			}
		} else {
			log.debug("No alien to rip of");
		}
	}

}
