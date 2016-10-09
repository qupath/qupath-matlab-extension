import com.mathworks.engine.MatlabEngine
import com.mathworks.matlab.types.Struct

import java.awt.image.BufferedImage

/**
 * Some static methods for connecting QuPath with MATLAB - via Groovy.
 * 
 * @author Pete Bankhead
 */
class QuPathGroovyMATLAB {

    private static PrintWriter writer;
    private static MatlabEngine engine;

    /**
     * Get a shared MATLAB engine.
     *
     * This is a static variable - the same engine will be returned each time, or null if no
     * shared engine could be found.
     *
     * Note that it is necessary to call
     * <code>
     *     matlab.engine.shareEngine
     * </code>
     * from within MATLAB to share the engine there.
     *
     * @param script The script requesting the engine. If provided, this helps redirect the output.
     * @return
     */
    public static MatlabEngine getEngine(Script script) {
        // Connect to a shared MATLAB instance
        if (engine == null) {
            try {
                engine = MatlabEngine.connectMatlab()
                this.writer = script == null ? new PrintWriter(System.out) : script.binding.getProperty("out")
            } catch (Exception e) {
                println(e.getMessage())
                e.printStackTrace()
            }
        }
        return engine
    }

    /**
     * Returns true if getEngine() returns an engine, or false if it returns null.
     * (This will result in an engine being claimed, even if this was not previously the case.)
     *
     * @return
     */
    public static boolean hasEngine() {
        return getEngine(null) != null;
    }

    /**
     * Returns result of hasEngine(), and also prints a message if no engine is available.
     *
     * @return
     */
    public static boolean checkHasEngine() {
        if (hasEngine())
            return true
        println("No MATLAB engine connected!")
        return false
    }

    /**
     * Put an RGB image in the MATLAB workspace as an m x n x 3 uint8 array.
     *
     * @param variable The variable name
     * @param img The image
     */
    public static void putRGBImage(String variable, BufferedImage img) {
        int w = img.getWidth()
        int h = img.getHeight()
        def pixels = img.getRGB(0, 0, w, h, null, 0, w)

        // Send RGB image to MATLAB, and reshape as required
        putVariable(variable, pixels)
        def size = "4, " + w + ", " + h

        eval(String.format("%s = reshape(typecast(%s, 'uint8'), %s);", variable, variable, size))
        eval(String.format("%s = permute(%s(3:-1:1,:,:), [3, 2, 1]);", variable, variable))
    }

    /**
     * Put a variable into the MATLAB workspace.
     *
     * @param name
     * @param variable
     */
    public static void putVariable(String name, Object variable) {
        if (!checkHasEngine())
            return
        engine.putVariable(name, variable)
    }

    /**
     * Retrieve a variable from the MATLAB workspace.
     *
     * @param name
     * @return
     */
    public static Object getVariable(String name) {
        if (!checkHasEngine())
            return
        return engine.getVariable(name)
    }

    /**
     * Evaluate a statement in the MATLAB workspace.
     *
     * @param statement
     * @return
     */
    public static boolean eval(String statement) {
        if (!checkHasEngine())
            return
        try {
            engine.eval(statement, writer, writer)
        } catch (Exception e) {
            writer.println("Exception running statement: " + statement)
//            writer.println(e.getMessage())
            writer.println("  Caused by " + e.getCause())
            throw (e)
//            e.printStackTrace(writer)
        }
    }

    public static Object feval(String method, Object... parameters) {
        if (!checkHasEngine())
            return
        try {
            return engine.feval(1, method, parameters)
        } catch (Exception e) {
            writer.println("Exception running function: " + method + " with parameters " + parameters.toArrayString())
//            writer.println(e.getMessage())
            writer.println("  Caused by " + e.getCause())
            throw (e)
//            e.print(writer)
        }
        return null
    }



    /**
     * Put an image in the MATLAB workspace, converting it into a multidimensional single array.
     *
     * @param variable The variable name
     * @param img The image
     */
    public static void putImage(String variable, BufferedImage img) {
        if (!checkHasEngine())
            return
        int w = img.getWidth()
        int h = img.getHeight()
        int numBands = img.getSampleModel().getNumBands()
        float[][] samples = new float[numBands][w*h]
        for (int b = 0; b < numBands; b++) {
            samples[b] = img.getRaster().getSamples(0, 0, w, h, b, (float[])null);
        }
        putVariable(variable, samples)
        def size = numBands + ", " + w + ", " + h
        eval(String.format("%s = reshape(%s, %s);", variable, variable, size))
        eval(String.format("%s = permute(%s, [3, 2, 1]);", variable, variable))
    }



    /**
     * Put a mask image in the MATLAB workspace, converting it into a 2D logical array.
     *
     * Nonzero entries will be true, zero entries will be false.
     *
     * @param variable The variable name
     * @param img The image
     */
    public static void putMaskImage(String variable, BufferedImage img) {
        if (!checkHasEngine())
            return
        // Bit of a hack... not very efficient, but at least the code is short
        putImage(variable, img)
        eval(String.format("%s = any(%s > 0, 3);", variable, variable))
    }

    /**
     * Create a structure in the MATLAB workspace to encapsulate the data relating to an image region.
     *
     * @param variable Name of the MATLAB variable
     * @param img Image to pass
     * @param imgMask Mask to pass - should be same size as img, or else null
     * @param x Pixel coordinate for the x origin (top left corner)
     * @param y Pixel coordinate for the y origin (top left corner)
     * @param downsample Downsample factor applied when extracting img from an original dataset
     */
    public static void putQuPathImageStruct(String variable, BufferedImage img, BufferedImage imgMask, double x, double y, double downsample, boolean doRGB) {
        if (!checkHasEngine())
            return

        // Create a temporary image variable & assign to MATLAB workspace
        String varImage = getUniqueVariableName("img")
        if (doRGB)
            putRGBImage(varImage, img)
        else
            putImage(varImage, img)

        // Create a temporary mask variable & assign to MATLAB workspace
        String varMask = getUniqueVariableName("mask")
        if (imgMask == null)
            putVariable(varMask, Boolean.TRUE)
        else
            putMaskImage(varMask, imgMask)

        // Add the main structure to the workspace
        Struct struct = new Struct("x", x, "y", y, "downsample", downsample)
        putVariable(variable, struct)

        // Add the image & mask fields within MATLAB
        eval(String.format("%s.im = %s;", variable, varImage))
        eval(String.format("%s.mask = %s;", variable, varMask))

        // Clear the image & mask temporary variables
        eval(String.format("clear %s %s", varImage, varMask))
    }



    /**
     * Get a unique variable name, which does not currently exist in the MATLAB workspace.
     *
     * @param prefix
     * @return
     */
    public static String getUniqueVariableName(final String prefix) {
        String[] variables = feval("who")
        String newName = prefix + Float.floatToIntBits((float)Math.random())
        if (variables == null)
            return newName
        while (variables.contains(newName))
            newName = prefix + Float.floatToIntBits((float)Math.random())
        return newName
    }



    /**
     * Stop the connection to the MATLAB engine.
     *
     * @return true if the engine was closed, false if there was some trouble or no engine to close.
     */
    public static boolean close() {
        if (engine != null) {
            // Try to disconnect
            try {
                engine.disconnect()
            } catch (Exception e) {
                e.printStackTrace(writer)
            }
            // Reset engine, regardless of whether disconnect succeeded
            engine = null
            writer = new PrintWriter(System.out)
            return true
        }
        return false
    }


}