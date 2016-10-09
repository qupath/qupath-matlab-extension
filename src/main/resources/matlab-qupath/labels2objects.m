function objects = labels2objects(L, region, measure_image)
% Convert a labelled image L into an array of STRUCTs, where each STRUCT 
% encapsulates the data to represent an object in L.
% 
% The purpose of this is to represent a stripped-down version of a QuPath 
% PathObject in a MATLAB-friendly way, so that it can be converted later if 
% necessary (i.e. to send it back to QuPath).
% 
% REGION is another STRUCT, as output from the CREATE_REGION_STRUCT function.
% MEASURE_IMAGE is a boolean; if true, and if REGION contains an IM field,
% then some basic measurements will be included as a sub-structur within
% each entry of OBJECTS.
%
% Author: Pete Bankhead, 2016

% Handle struct region, or raw image
if nargin < 2 || isempty(region)
    im = [];
    mask = true;
    x = 0;
    y = 0;
    downsample = 1;
elseif isstruct(region)
    if isfield(region, 'mask')
        mask = region.mask;
    else
        mask = true;
    end
    if nargin >= 3 && measure_image && isfield(region, 'im')
        im = region.im;
    else
        im = [];
    end
    x = region.x;
    y = region.y;
    downsample = region.downsample;
else
    error('region should be a struct with fields mask (optional), x, y and downsample');
end

% Apply mask to labelled image, if available
if ~isscalar(mask)
    L(~mask) = 0;
end

% Compute stats to allow superpixels to be converted to boundaries
stats = regionprops(L, 'FilledImage', 'BoundingBox', 'PixelIdxList');
objects = struct('x', [], 'y', [], 'measurements', []);

% Loop through and get boundaries
counter = 1;
for ii = 1:numel(stats)
    bw = stats(ii).FilledImage;
    bounds = stats(ii).BoundingBox;
    
    % Check we have pixels - might not if a mask was applied
    if isempty(stats(ii).PixelIdxList)
        continue
    end
    
    % MATLAB provides the centre of each boundary pixel with BWBOUNDARIES
    % We've now got a bit of work to do to make this an outer boundary...
    upsample = 3;
    bw_upsampled = imresize(bw, upsample,'nearest');
    boundaries = bwboundaries(bw_upsampled);
    boundary = [];
    % Take the largest boundary
    % (There's probably only one, but because of masking this could change)
    for bb = 1:numel(boundaries)
        if numel(boundaries{bb}) > numel(boundary)
            boundary = boundaries{bb};
        end
    end
    boundary = round(boundary/upsample);
    
    % Create object, adjusting coordinates as required
    object = struct;
    object.x = single(boundary(:,2) + floor(bounds(1))) * downsample + x;
    object.y = single(boundary(:,1) + floor(bounds(2))) * downsample + y;
    
    % Add basic measurements, if image is available
    measurements = struct;
    if ~isempty(im)
        for cc = 1:size(im, 3)
            im_channel = im(:,:,cc);
            pixels = single(im_channel(stats(ii).PixelIdxList));
            measurements.(['Channel_', num2str(cc), '_mean']) = mean(pixels);
            measurements.(['Channel_', num2str(cc), '_std_dev']) = std(pixels);
        end
    end
    object.measurements = measurements;
    
    % Store object for output
    objects(counter) = object;
    counter = counter + 1;
end