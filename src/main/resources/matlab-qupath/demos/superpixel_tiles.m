function [tiles, L] = superpixel_tiles(region, N, varargin)
% Helper function for computing superpixel tiles from a region struct 
% (see CREATE_REGION_STRUCT) that contains pixel data, i.e. an IM field.
% 
% It is intended as an example to demonstrate the use of the MATLAB Java
% Engine, along with MATLAB_superpixels.groovy script in QuPath.
% 
% This function calls MATLAB's SUPERPIXELS function, which provides more
% information regarding the N an VARARGIN inputs.
% 
% INPUT:
%    REGION - Image region struct.  See CREATE_REGION_STRUCT for more
%             details.
%    N      - The requested number of superpixels.
% 
% OUTPUT:
%    TILES - An array of object STRUCTs, as output from LABELS2OBJECTS.
%    L     - A labelled image, corresponding to the created tiles.
% 
% Author: Pete Bankhead, 2016


% Handle struct region, or raw image
if isstruct(region)
    im = region.im;
elseif isnumeric(region)
    im = region;
else
    error('region should be a matrix or pixel values, or a struct with fields im, mask, x, y and downsample');
end

% Calculate N if needed
if nargin < 2 || isempty(N)
    N = round(numel(im) / (50*50));
end


% for cc = 1:size(im,3)
%     im(:,:,cc) = medfilt2(im(:,:,cc), [9, 9]);
% end


% Calculate superpixels
L = superpixels(im, N, varargin{:});

% Calculate tiles
tiles = labels2objects(L, region);