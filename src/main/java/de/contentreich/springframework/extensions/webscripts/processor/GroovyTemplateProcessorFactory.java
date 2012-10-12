/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This file is part of the Spring Surf Extension project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.contentreich.springframework.extensions.webscripts.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.SearchPath;
import org.springframework.extensions.webscripts.TemplateProcessor;
import org.springframework.extensions.webscripts.TemplateProcessorFactory;

/**
 * @author muzquiano
 */
public class GroovyTemplateProcessorFactory implements TemplateProcessorFactory, ApplicationContextAware
{
	private static final Log logger = LogFactory.getLog(GroovyTemplateProcessorFactory.class);
	
	private static final String WEBSCRIPTS_SEARCHPATH_ID = "webscripts.searchpath";

	protected ApplicationContext applicationContext;
	protected SearchPath searchPath = null;
	protected GroovyScriptProcessorFactory scriptProcessorFactory = null;
	protected String defaultEncoding = null;	
	
	public void setApplicationContext(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}
	
	public void setScriptProcessorFactory(GroovyScriptProcessorFactory scriptProcessorFactory)
	{
		this.scriptProcessorFactory = scriptProcessorFactory;
	}
	
	public void setDefaultEncoding(String defaultEncoding)
	{
		this.defaultEncoding = defaultEncoding;
	}
	
	public void setSearchPath(SearchPath searchPath)
	{
		this.searchPath = searchPath;
	}

	/* (non-Javadoc)
	 * @see org.springframework.extensions.webscripts.TemplateProcessorFactory#newInstance()
	 */
	public TemplateProcessor newInstance()
	{
		if (searchPath == null)
		{
			searchPath = (SearchPath) applicationContext.getBean(WEBSCRIPTS_SEARCHPATH_ID);
		}
		
		GroovyTemplateProcessor groovyTemplateProcessor = new GroovyTemplateProcessor();
		groovyTemplateProcessor.setDefaultEncoding(defaultEncoding);
		groovyTemplateProcessor.setSearchPath(searchPath);
		
		GroovyScriptProcessor groovyScriptProcessor = (GroovyScriptProcessor) this.scriptProcessorFactory.newInstance();
		groovyScriptProcessor.setSearchPath(searchPath);
		groovyTemplateProcessor.setGroovyScriptProcessor(groovyScriptProcessor);
		
		return groovyTemplateProcessor;
	}	
}
