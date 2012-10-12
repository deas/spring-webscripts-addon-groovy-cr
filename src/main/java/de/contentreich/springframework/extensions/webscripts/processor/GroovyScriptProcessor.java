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

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.core.scripts.ScriptException;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.processor.AbstractScriptProcessor;

public class GroovyScriptProcessor extends AbstractScriptProcessor implements InitializingBean {
	private static final Log log = LogFactory
			.getLog(GroovyScriptProcessor.class);

	private boolean prependPackage = false;
	private Map<String, Object> allProcessorExtensions;
	private List<URL> extraClassPath;
	private GroovyClassLoader shellClassLoader = null;

	public void setPrependPackage(boolean prependPackage) {
		this.prependPackage = prependPackage;
	}

	public void setExtraClassPath(List<URL> extraClassPath) {
		this.extraClassPath = extraClassPath;
	}

	public GroovyScriptProcessor(
			Map<String, Object> otherProcessorExtensions) {
		this.allProcessorExtensions = new HashMap<String, Object>();
		this.allProcessorExtensions.putAll(processorExtensions);
		if (otherProcessorExtensions != null) {
			for (String name : otherProcessorExtensions.keySet()) {
				if (this.allProcessorExtensions.containsKey(name)) {
					log.warn("Keeping my own extension " + name
							+ " instead of using the alien version");
				} else {
					this.allProcessorExtensions.put(name,
							otherProcessorExtensions.get(name));
				}
			}
			log.debug(this.allProcessorExtensions.keySet().size()
					- processorExtensions.keySet().size()
					+ " alien processor extensions added");
		} else {
			log.debug("No alien process extensions");
		}
	}

	@Override
	public ScriptContent findScript(String path) {
		int i = path.indexOf(".groovy");
		String groovyPath = path.substring(0, i).replaceAll("\\.", "_")
				+ ".groovy";
		return getScriptLoader().getScript(groovyPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.extensions.surf.core.processor.Processor#getExtension
	 * ()
	 */
	public String getExtension() {
		return "groovy";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.extensions.surf.core.processor.Processor#getName()
	 */
	public String getName() {
		return "groovy";
	}

	/**
	 * Executes the Groovy script
	 * 
	 * @param is
	 *            the input stream
	 * @param out
	 *            the writer. This can be null if no output is required.
	 * @param model
	 *            the context model for the script
	 * @return Object the return result of the executed script
	 */
	private Object executeGroovyScript(InputStream is, Writer out,
			Map<String, Object> model) {
		try {
			GroovyShell shell = new GroovyShell();
			// new GroovyCodeSource(script, name, codeBase)
			Script script = shell.parse(is);
			this.addProcessorModelExtensions(model);

			Binding binding = new Binding(model);
			for (String name : processorExtensions.keySet()) {
				binding.setProperty(name, processorExtensions.get(name));
			}
			binding.setProperty("out", out);
			script.setBinding(binding);

			return script.run();
		} catch (Exception exception) {
			throw new ScriptException("Error executing groovy script",
					exception);
		}
	}

	/**
	 * @see org.alfresco.service.cmr.repository.ScriptProcessor#executeString(java.lang.String,
	 *      java.util.Map)
	 */
	public Object executeGroovyString(String script, Map<String, Object> model) {
		return executeGroovyScript(new ByteArrayInputStream(script.getBytes()),
				null, model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.extensions.webscripts.processor.AbstractScriptProcessor
	 * #init()
	 */
	public void init() {
		super.init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.extensions.webscripts.ScriptProcessor#executeScript
	 * (java.lang.String, java.util.Map)
	 */
	public Object executeScript(String path, Map<String, Object> model) {
		ScriptContent scriptContent = findScript(path);
		if (scriptContent == null) {
			throw new ScriptException("Unable to locate: " + path);
		}

		return executeScript(scriptContent, model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.extensions.webscripts.ScriptProcessor#unwrapValue
	 * (java.lang.Object)
	 */
	public Object unwrapValue(Object value) {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.extensions.webscripts.ScriptProcessor#reset()
	 */
	public void reset() {
		init();
	}

	@Override
	public Object executeScript(ScriptContent scriptContent,
			Map<String, Object> model) {
		String cp = scriptContent.getPathDescription().replaceFirst(
				"classpath\\*{0,1}:", "");
		String filename = null;
		InputStream is = null;
		String pkg = null;
		int idx = cp.lastIndexOf("/");
		if (this.prependPackage) {
			pkg = cp.substring(0, idx).replace('/', '.');
			String imp = "package " + pkg + ";";
			filename = cp.substring(idx + 1, cp.length());
			is = new SequenceInputStream(new ByteArrayInputStream(
					imp.getBytes()), scriptContent.getInputStream());
		} else {
			filename = (idx > -1) ? cp.substring(idx + 1, cp.length()) : cp;
			is = scriptContent.getInputStream();
		}
		log.debug("Executing script with filename " + filename
				+ ", setting package " + ((pkg != null) ? pkg : "(none)"));
		// scriptContent.getPathDescription()
		// classpath*:alfresco/templates/webscripts/test/groovy1_get.groovy
		// getPr
		return executeGroovyScript(is, filename, null, model);
	}

	// TODO : cache
	private Object executeGroovyScript(InputStream is, String file, Writer out,
			Map<String, Object> model) {
		try {
			GroovyShell shell = new GroovyShell(shellClassLoader);
			Script script = null;
			if (file != null) {
				script = shell.parse(new InputStreamReader(is), file);
			} else {
				script = shell.parse(new InputStreamReader(is));
			}
			this.addProcessorModelExtensions(model);

			Binding binding = new Binding(model);
			for (String name : this.allProcessorExtensions.keySet()) {
				binding.setProperty(name, this.allProcessorExtensions.get(name));
			}
			binding.setProperty("out", out);
			script.setBinding(binding);

			return script.run();
		} catch (Exception exception) {
			throw new ScriptException("Error executing groovy script",
					exception);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		shellClassLoader = new GroovyClassLoader();
		for (URL url : extraClassPath) {
			shellClassLoader.addURL(url);
		}
	}

}
