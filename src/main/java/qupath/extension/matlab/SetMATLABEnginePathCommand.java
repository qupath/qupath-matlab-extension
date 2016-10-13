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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.StringProperty;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.interfaces.PathCommand;
import qupath.lib.gui.helpers.DisplayHelpers;
import qupath.lib.gui.prefs.PathPrefs;

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
	
	final private static StringProperty matlabEnginePath = PathPrefs.createPersistentPreference("matlabEnginePath", null);
	
	private QuPathGUI qupath;
	
	public SetMATLABEnginePathCommand(final QuPathGUI qupath) {
		this.qupath = qupath;
		// Add MATLAB engine path
		updateExtensionPath();
		// Listen for changes to path property
		matlabEnginePath.addListener((v, o, n) -> updateExtensionPath());
	}

	private void updateExtensionPath() {
		String path = matlabEnginePath.get();
		if (path != null && new File(path).exists()) {
			qupath.addExtensionJar(new File(path));
		}
	}

	@Override
	public void run() {
		// Prompt to select MATLAB engine file
		File fileEngine = qupath.getDialogHelper().promptForFile("Select MATLAB engine", null, "MATLAB engine", new String[]{"jar"});	
		if (fileEngine == null || !fileEngine.isFile()) {
			logger.warn("No MATLAB engine selected");
		}
		
		// Check name
		if (!fileEngine.getName().equals("engine.jar")) {
			if (!DisplayHelpers.showConfirmDialog("Set MATLAB engine", "This looks like the wrong file!\nThe MATLAB engine should be called engine.jar, but you selected " + fileEngine.getName() + "\n\nTry to link anyway?"))
				return;
		}

		// Set the path
		matlabEnginePath.set(fileEngine.getAbsolutePath());
		qupath.refreshExtensions(false);
	}

}
