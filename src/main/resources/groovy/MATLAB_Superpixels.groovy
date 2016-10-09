/**
 * Demo script sending an image to MATLAB for the generation of superpixels.
 *
 * This requires the MATLAB Engine, available with MATLAB R2016b,
 * and setup as described at https://github.com/qupath/qupath/wiki/Working-with-MATLAB
 *
 * @author Pete Bankhead
 */

import com.mathworks.matlab.types.Struct
import qupath.lib.images.tools.BufferedImageTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathTileObject
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.PolygonROI
import qupath.lib.scripting.QP
import qupath.extension.matlab.QuPathMATLABExtension

import java.awt.image.BufferedImage

// Import the helper class
QuPathMATLAB = this.class.classLoader.parseClass(QuPathMATLABExtension.getQuPathMATLABScript())

// Get the MATLAB engine
QuPathMATLAB.getEngine(this)
try {

    def imageData = QP.getCurrentImageData()

    double downsample = 8
    BufferedImage img;
    def roi = QP.getSelectedROI()
    if (roi != null) {
        img = imageData.getServer().readBufferedImage(RegionRequest.createInstance(imageData.getServerPath(), downsample, roi))
    } else
        img = imageData.getServer().getBufferedThumbnail(1000, -1, 0)

    // Put RGB version of QuPath image & calibration data into MATLAB workspace
    if (roi == null) {
        double downsampleH = (double)imageData.getServer().getHeight()/(double)img.getHeight()
        double downsampleW = (double)imageData.getServer().getWidth()/(double)img.getWidth()
        double downsampleActual = Math.max(downsampleH, downsampleW)
        QuPathMATLAB.putQuPathImageStruct("img", img, null, 0, 0, downsampleActual, true)
    }
    else {
        def imgMask = BufferedImageTools.createROIMask(img.getWidth(), img.getHeight(), roi, roi.getBoundsX(), roi.getBoundsY(), downsample)
        QuPathMATLAB.putQuPathImageStruct("img", img, imgMask, roi.getBoundsX(), roi.getBoundsY(), downsample, true)
    }
    
    // Compute superpixel tiles
	// By writing a function in MATLAB, this lets the 'hard work' to be done there - 
	// before pulling back the results into this script for handling in QuPath
    QuPathMATLAB.eval("tiles = superpixel_tiles(img);")
    
    // Remove any previous tiles from currently-selected object
    def parent = QP.getSelectedObject()
    if (parent == null) {
        QP.clearAllObjects()
        parent = QP.getCurrentHierarchy().getRootObject()
    } else
        parent.clearPathObjects()

    def tiles = QuPathMATLAB.getVariable("tiles")

    def tileObjects = new ArrayList<PathObject>()
    for (Struct tile in tiles) {
        def tileROI = new PolygonROI(tile.get("x"), tile.get("y"), 0, 0, 0)
        def tileObject = new PathTileObject(tileROI);
        // Add measurements, if available
        if (tile.containsKey("measurements")) {
            Struct measurements = tile.get("measurements")
            // Unfortunately, MATLAB's Struct seems to use a HashMap or similar, i.e. unordered keys
            // To bring a bit more order to this, try sorting the keys here
            List<String> measurementNames = new ArrayList<>(measurements.keySet())
            Collections.sort(measurementNames)
            def measurementList = tileObject.getMeasurementList()
            for (String name : measurementNames) {
                def value = measurements.get(name)
                // Only add in numeric values - with a little bit of cleanup in the naming
                // (MATLAB's structs can't contain spaces, so underscores are a workaround)
                if (value instanceof Number)
                    measurementList.putMeasurement(name.replace("_", " "), value)
            }
        }
        tileObjects.add(tileObject);
    }

	// Add the tiles
    parent.addPathObjects(tileObjects)
    if (parent.isAnnotation() || parent.isTMACore())
        parent.setLocked(true);
		
	// Fire a change event
    QP.getCurrentHierarchy().fireHierarchyChangedEvent(this)

    print("Done!")
} catch (Exception e) {
    println("Error running script " + e.getMessage())
} finally {
    QuPathMATLAB.close()
}