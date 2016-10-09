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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.interfaces.PathCommand;

/**
 * Command to help with the setup of QuPath and MATLAB integration.
 * 
 * @author Pete Bankhead
 *
 */
public class MATLABQuPathSetupCommand implements PathCommand {
	
	private final static Logger logger = LoggerFactory.getLogger(MATLABQuPathSetupCommand.class);
	
	private QuPathGUI qupath;
	
	public MATLABQuPathSetupCommand(final QuPathGUI qupath) {
		this.qupath = qupath;
	}

	public void run() {
		
		File dirMATLAB = qupath.getDialogHelper().promptForDirectory(null);
		if (dirMATLAB == null)
			return;
		
		// Get a map of all MATLAB files
		String scriptPath = "matlab-qupath/";
		Map<String, String> scriptMap = QuPathMATLABExtension.readScriptMap(scriptPath, ".m");
		
		// Write into the selected directory
		for (Entry<String, String> entry : scriptMap.entrySet()) {
			
			File fileOutput = dirMATLAB;
			for (String part : entry.getKey().split("/")) {
				if (!part.isEmpty())
					fileOutput = new File(fileOutput, part);
			}
			logger.info(fileOutput.getAbsolutePath());
			
			// Ensure the directory exists
			if (!fileOutput.getParentFile().exists())
				fileOutput.getParentFile().mkdirs();
			
			// Write the script
			try (PrintWriter writer = new PrintWriter(fileOutput)) {
				writer.print(entry.getValue());
			} catch (FileNotFoundException e) {
				logger.error("Error writing file", e);
			}
		}
		
	}
	
	
}
