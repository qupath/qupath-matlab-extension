% An example of a simple MATLAB script for processing TMA cores with QuPath.
% 
% Here, TMA cores are simply thresholded after filtering, and 
% the resulting detected region sent back to QuPath as a detection object.
%
% Author: Pete Bankhead, 2016

% Get all the cores
cores = QuPath.getTMACores();

% Loop through cores
for ii = 1:numel(cores)

    % Get the current core object
    core = cores{ii}; 
    
    % Display the core name
    disp(core);
    
    % Request (downsampled) image for the core
    img = QuPath.getImage(core.getROI(), 4);
    
    % Create smoothed image
    im2 = imfilter(single(mean(img, 3)), fspecial('gaussian', 25, 3), 'symmetric');
    
    % Threshold using mean & standard deviation
    bw = im2 < mean(im2(:)) - std(im2(:));
    
    % Remove very small areas
    bw = bwareaopen(bw, 5);
    
    % Add detection objects back into QuPath
    QuPath.addDetections(core, bw);
end