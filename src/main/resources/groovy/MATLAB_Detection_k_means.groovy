/**
 * Demo script to apply k-means clustering using MATLAB to the measurements of
 * QuPath detection objects.
 *
 * This requires the MATLAB Engine, available with MATLAB R2016b,
 * and setup as described at https://github.com/qupath/qupath/wiki/Working-with-MATLAB
 *
 * @author Pete Bankhead
 */

import qupath.lib.classifiers.PathClassificationLabellingHelper
import qupath.lib.common.ColorTools
import qupath.lib.objects.classes.PathClass
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.scripting.QP
import qupath.extension.matlab.QuPathMATLABExtension

//------------------------------

// Define the number of clusters to use
int nClusters = 5;

//------------------------------

// Import the helper class
QuPathMATLAB = this.class.classLoader.parseClass(QuPathMATLABExtension.getQuPathMATLABScript())

// Get the MATLAB engine, passing this script as a parameter
// This is important to ensure print statements are redirected
QuPathMATLAB.getEngine(this)
try {

    // Get the detection objects
    def detections = QP.getDetectionObjects()
    if (detections.isEmpty()) {
        println("No detection objects found!")
        return
    }

    // Get the names of all measurements
    def featureNames = PathClassificationLabellingHelper.getAvailableFeatures(detections).toArray(new String[0])
    if (featureNames.length == 0) {
        println("No measurements to cluster!")
        return
    }

    // Create a new PathClass for each cluster, with a random color
    List<PathClass> clusterClasses = new ArrayList<>()
    for (int i = 0; i < nClusters; i++) {
        def pathClass = PathClassFactory.getPathClass("Cluster " + (i+1));
        // Update the color (in case the PathClass already existed)
        pathClass.setColor(ColorTools.makeRGB(
                (int) (255 * Math.random()),
                (int) (255 * Math.random()),
                (int) (255 * Math.random())))
        clusterClasses.add(pathClass)
    }

    // Create an array of features for each object
    float[][] featureArray = new float[detections.size()][featureNames.size()]
    int i = 0
    for (def detection in detections) {
        int j = 0;
        float[] arr = featureArray[i]
        for (def feature in featureNames) {
            arr[j] = detection.getMeasurementList().getMeasurementValue(feature)
            j++
        }
        featureArray[i] = arr
        i++
    }

    // Set features to MATLAB
    QuPathMATLAB.putVariable("featureNames", featureNames)
    QuPathMATLAB.putVariable("features", featureArray)

    // Normalize features, by subtracting mean and dividing by standard deviation
    QuPathMATLAB.eval("features = bsxfun(@minus, features, nanmean(features));")
    QuPathMATLAB.eval("features = bsxfun(@rdivide, features, nanstd(features));")

    // Fill in nans... not particularly elegantly, but since means are all now 0
    // we are effectively filling with the mean value
    QuPathMATLAB.eval("features(isnan(features)) = 0;")

    // Apply k-means clustering
    QuPathMATLAB.eval("clusters = kmeans(features, " + nClusters + ");")

    // Set the PathClass according to the cluster
    def clusters = QuPathMATLAB.getVariable("clusters")
    for (i = 0; i < detections.size(); i++) {
        def ind = clusters[i]
        def pathClass = clusterClasses.get((int)(ind-1))
        detections.get(i).setPathClass(pathClass)
    }

    // Make sure an update is triggered
    QP.fireHierarchyUpdate()

    // Print a message so we know we reached the end
    print("Clustering complete with " + nClusters + " clusters")
} catch (Exception e) {
    println("Error running script: " + e.getMessage())
} finally {
    QuPathMATLAB.close()
}