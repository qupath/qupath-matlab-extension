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
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.interfaces.PathCommand;
import qupath.lib.gui.helpers.DisplayHelpers;

/**
 * Command to help with putting the MATLAB engine JAR on the classpath used by QuPath.
 * 
 * In practice, this tries first to create a symbolic link to the extensions directory - 
 * and copies the JAR if that doesn't work out.
 * 
 * A clearer solution in the future would be preferable...
 * 
 * @author Pete Bankhead
 *
 */
public class SetMATLABEnginePathCommand implements PathCommand {
	
	private static Logger logger = LoggerFactory.getLogger(SetMATLABEnginePathCommand.class);
	
	private QuPathGUI qupath;
	
	public SetMATLABEnginePathCommand(final QuPathGUI qupath) {
		this.qupath = qupath;
	}

	@Override
	public void run() {
		// Prompt to select MATLAB engine file
		File fileEngine = qupath.getDialogHelper().promptForFile("Select MATLAB engine", null, "MATLAB engine", new String[]{"jar"});	
		if (fileEngine == null) {
			logger.warn("No MATLAB engine selected");
		}
		// Check name
		if (!fileEngine.getName().equals("engine.jar")) {
			if (!DisplayHelpers.showConfirmDialog("Set MATLAB engine", "This looks like the wrong file!\nThe MATLAB engine should be called engine.jar, but you selected " + fileEngine.getName() + "\n\nTry to link anyway?"))
				return;
		}
		
		// Delete any existing file or link, if necessary
		File fileLink = new File(QuPathGUI.getExtensionDirectory(), "matlab_engine");
		if (fileLink.exists())
			fileLink.delete();
		
		// Try to create a symbolic link to the directory first
		try {
			Files.createSymbolicLink(fileEngine.getParentFile().toPath(), fileLink.toPath());
			qupath.refreshExtensions(false);
			logger.error("Symbolic link created at " + fileLink.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Unable to link to " + fileEngine.getAbsolutePath(), e);
			try {
				// If that didn't work, try copying the JAR
				fileLink = new File(QuPathGUI.getExtensionDirectory(), "matlab_engine.jar");
				Files.copy(fileEngine.toPath(), fileLink.toPath());
				qupath.refreshExtensions(false);
				logger.error("MATLAB engine copied to " + fileLink.getAbsolutePath());
			} catch (IOException e1) {
				logger.error("Error copying MATLAB engine", e1);
				DisplayHelpers.showErrorMessage("Set MATLAB engine", "Unable to copy MATLAB engine to QuPath extension directory - please try to copy or link the file manually");
			}
		}
	}

}
