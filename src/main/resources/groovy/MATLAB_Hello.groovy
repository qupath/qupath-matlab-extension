/**
 * Demo script to check QuPath can talk to MATLAB in a friendly way.
 *
 * This requires the MATLAB Engine, available with MATLAB R2016b,
 * and setup as described at https://github.com/qupath/qupath/wiki/Working-with-MATLAB
 *
 * @author Pete Bankhead
 */

// Import the helper class
import qupath.extension.matlab.QuPathMATLABExtension;
QuPathMATLAB = this.class.classLoader.parseClass(QuPathMATLABExtension.getQuPathMATLABScript())

// Get the MATLAB engine
// Currently, it's essential to make sure close() is called -
// so the try/finally is important.
// Without it, there's a risk of an error that will prevent reconnection...
QuPathMATLAB.getEngine(this)
try {
	QuPathMATLAB.putVariable('a', 'Hello from QuPath!')
	QuPathMATLAB.eval('disp(a);')
} finally {
	QuPathMATLAB.close()
}
