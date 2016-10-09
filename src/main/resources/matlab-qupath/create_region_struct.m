function region = create_region_struct(downsample, x, y, im, mask)
% Create a STRUCT that contains the minimal information related to a
% (QuPath) image region.
% 
% The purpose is to make it possible to work with an images extracted from 
% a (possibly much larger) image at a specified magnification, and then be 
% able to transform coordinates from the extracted image back to match the
% original from which it came.
%
% This makes it possible to (for example) detect regions in the image
% region, and then convert these into PathObjects for sending to QuPath.
%
% The output STRUCT may optionally contain pixel values and a mask as well.  
% However, not all commands that require a region struct also require pixel 
% values (e.g. if the struct is only needed to determine an appropriate 
% coordinate transform).
% 
% INPUT:
%     DOWNSAMPLE - The downsampling applied when the region was extracted;
%                  choose 1 if the full resolution image is used.
%     X          - The x-coordinate of the top left of the region bounding
%                  box in the full-resolution image, defined in pixels.
%     Y          - The y-coordinate of the top left of the region bounding
%                  box in the full-resolution image, defined in pixels.
%     IM         - Pixel values for the image region (optional).
%     MASK       - A binary mask, the same size of IM, for the image
%                  region (optional).
%
% Author: Pete Bankhead, 2016

% Default to having no image
if nargin < 4
    im = [];
end

% Default to having no mask
if nargin < 5
    mask = [];
end

% Create the region structure
region = struct('im', im, 'mask', mask, 'downsample', downsample, 'x', x, 'y', y);