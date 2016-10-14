/*-
 * #%L
 * This file is part of a QuPath extension.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.extension.matlab;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.OpenWebpageCommand;
import qupath.lib.gui.extensions.QuPathExtension;

/**
 * QuPath extension to help facilitate integration between QuPath and MATLAB.
 * 
 * This is mostly concerned with providing scripts, and moving them into the right places.
 * 
 * @author Pete Bankhead
 *
 */
public class QuPathMATLABExtension implements QuPathExtension {

	private static Logger logger = LoggerFactory.getLogger(QuPathMATLABExtension.class);
	
	public void installExtension(QuPathGUI qupath) {
		QuPathGUI.addMenuItems(
				qupath.getMenu("Extensions>MATLAB", true),
				QuPathGUI.createCommandAction(new OpenWebpageCommand(qupath, "http://go.qub.ac.uk/qupath-matlab"), "QuPath-MATLAB documentation (web)")
				);
		
		QuPathGUI.addMenuItems(
				qupath.getMenu("Extensions>MATLAB", true),
				QuPathGUI.createCommandAction(new MATLABQuPathSetupCommand(qupath), "Export MATLAB scripts")
				);
		
		// Add script for setting engine path
		QuPathGUI.addMenuItems(
				qupath.getMenu("Extensions>MATLAB", true),
				QuPathGUI.createCommandAction(new SetMATLABEnginePathCommand(qupath), "Set path to MATLAB engine"),
				null
				);
		
		// Link useful scripts
		for (Entry<String, String> entry : readScriptMap("groovy", ".groovy").entrySet()) {
			Menu menuGroovy = qupath.getMenu("Extensions>MATLAB>Groovy MATLAB samples", true);
			String scriptName = entry.getKey().replaceAll("_", " ").replaceAll("/", " ").trim();
			if (scriptName.toLowerCase().endsWith(".groovy"))
				scriptName = scriptName.substring(0, scriptName.length() - ".groovy".length());
			MenuItem item = new MenuItem(scriptName);
			item.setOnAction(e -> {
				qupath.getScriptEditor().showScript(entry.getKey(), entry.getValue());
			});
			QuPathGUI.addMenuItems(menuGroovy, item);
		}
		
	}

	public String getName() {
		return "QuPath MATLAB extension";
	}

	public String getDescription() {
		return "Helps facilitate integration between QuPath and MATLAB";
	}
	
	
	/**
	 * Get a Groovy script to help with setup.
	 * 
	 * Potential usage within Groovy is:
	 * 
	 * 	 String helperScript = QuPathMATLABExtension.getQuPathMATLABScript()
	 *   QuPathMATLAB = this.class.classLoader.parseClass(getQuPathMATLABScript())
	 *   
	 * From then on, QuPathMATLAB is a helper script with some useful methods.
	 * 
	 * @return
	 */
	public static String getQuPathMATLABScript() {
		try {
			return GeneralTools.readInputStreamAsString(MATLABQuPathSetupCommand.class.getResourceAsStream("/groovy/QuPathMATLAB.groovy"));
		} catch (Exception e) {
			logger.error("Unable to read QuPath-MATLAB script", e);
			return null;
		}
	}
	
	
	
	
	/**
	 * Read scripts from resources directory, putting them into a map of name and full script text.
	 * 
	 * @param scriptPath
	 * @param ext
	 * @return
	 */
	static Map<String, String> readScriptMap(final String scriptPath, final String ext) {
		Map<String, String> scriptMap = new LinkedHashMap<>();
		try {
			// Load sample scripts
			File codeLocation = new File(QuPathMATLABExtension.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			if (codeLocation.isFile()) {
				// Read scripts from Jar file
				JarFile jar = new JarFile(codeLocation);
				for (JarEntry jarEntry : Collections.list(jar.entries())) {
					String name = jarEntry.getName();
					if (name.startsWith(scriptPath) && name.toLowerCase().endsWith(ext)) {
						try {
							String scriptText = GeneralTools.readInputStreamAsString(jar.getInputStream(jarEntry));
							String scriptName = name.substring(scriptPath.length());
							scriptMap.put(scriptName, scriptText);
							logger.info(scriptName);
							logger.debug("Read script from Jar: {}", name);
						} catch (IOException e) {
							logger.error("Error reading script from Jar", e);
						}
					}
				}
				jar.close();
			} else {
				// Read scripts from directory
				URL url = QuPathMATLABExtension.class.getClassLoader().getResource(scriptPath);
				for (File file : new File(url.toURI()).listFiles((File f) -> f.isFile() && f.getName().toLowerCase().endsWith(".m"))) {
					scriptMap.put(file.getName(), GeneralTools.readFileAsString(file.getAbsolutePath()));
					logger.debug("Read script: {}", file.getName());
				}
			}
		} catch (Exception e) {
			logger.error("Error reading scripts", e);
		}
		return scriptMap;
	}

}
