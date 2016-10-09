% An example of a simple MATLAB script to compute superpixels for an 
% image (or region) selected within QuPath, and send these back to QuPath.
%
% Author: Pete Bankhead, 2016

% Choose maximum size of image (either width or height)
max_dim = 1000;

% Choose approximate size of superpixel, in pixels
superpixel_size = 400;

% Get the image server
server = QuPath.getServer();

% Request the current selected object
parent = QuPath.getSelectedObject();
if isempty(parent)
    QuPath.getHierarchy().clearAll();
    roi = QuPath.createRectangle(0, 0, server.getWidth(), server.getHeight());
    parent = QuPath.createAnnotationObject(roi);
    QuPath.addObject(parent);
else
    % Remove any existing child objects
    parent.clearPathObjects();
    roi = parent.getROI();
end

% Figure out how much to downsample when requesting the image
downsample = max(roi.getBoundsWidth(), roi.getBoundsHeight()) / max_dim;
downsample = max(downsample, 1);

% Get the image pixels
im = QuPath.getImage(roi, downsample);

% Average together multiple channels, if necessary
im = mean(single(im), 3);

% Apply a small Gaussian filter
im = imgaussfilt(im, 1);

% Calculate superpixels
L = superpixels(im, max(5, round(numel(im)/superpixel_size)));

% Add detection objects back into QuPath
QuPath.addDetections(parent, L);