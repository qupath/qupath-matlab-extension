classdef (Sealed) QuPath < handle
% MATLAB class to help with launching and interacting with QuPath from the
% MATLAB command line.
% 
% To start, type something like the following:
%  
%   qupath = QuPath.getInstance()
%
% If everything has been correctly setup (see SETUP_QUPATH) then this
% should launch a QuPath instance.
%
% This can then be controlled, e.g. with commands such as
% 
%   qupath.getServer()                        % Get the current server name
%   q.addRectangleAnnotation(10, 5, 200, 200) % Add a new annotation
%
% Author: Pete Bankhead, 2016
    
    methods (Access = private)
        function obj = QuPath
        end
    end
    
    
    methods (Static)
        function obj = getInstance
            persistent localObj
            if isempty(localObj) || ~isvalid(localObj)
                localObj = QuPath;
            end
            % Get Java version
            v = version('-java');
            if isempty(strfind(v, 'Java 1.8'))
                if ispc
                    web('http://uk.mathworks.com/matlabcentral/answers/130359-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-on-windows');
                elseif ismac
                    web('http://uk.mathworks.com/matlabcentral/answers/103056-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-for-mac-os');
                else
                    web('http://uk.mathworks.com/matlabcentral/answers/130360-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-for-linux-os');
                end
                error('QuPath requires Java 1.8, but current version is %s\nTry starting MATLAB with a Java 8 Runtime Environment', v);
            end
            
            obj = localObj;
            
            % Add extensions directory to classpath
            dirExtensions = qupath.lib.gui.QuPathGUI.getExtensionDirectory();
            if ~isempty(dirExtensions)
                files = dirExtensions.listFiles();
                for ii = 1:numel(files)
                    f = files(ii);
                    if f.getName().toLowerCase().endsWith('.jar')
                        javaaddpath(char(f.getAbsolutePath()));
                    end
                end
            end
            
            % Put in launch request
            %             try
            disp('Launching QuPath...');
            qupath.lib.gui.QuPathGUI.launchQuPath();
            %             catch err
            %                 disp(err.stack);
            %                 error('Could not launch QuPath - have you checked output of setup_qupath.m to ensure paths & Java version are correct?');
            %             end
        end
        
        function obj = getQuPathGUI()
            obj = qupath.lib.gui.QuPathGUI.getInstance();
            if isempty(obj)
                qupath.lib.gui.QuPathGUI.launchQuPath();
            end
            obj = qupath.lib.gui.QuPathGUI.getInstance();
        end
        
        function imageData = getImageData()
            gui = QuPath.getQuPathGUI();
            if isempty(gui)
                imageData = [];
            else
                imageData = gui.getImageData();
            end
        end
        
        function hierarchy = getHierarchy()
            imageData = QuPath.getImageData();
            if isempty(imageData)
                hierarchy = [];
            else
                hierarchy = imageData.getHierarchy();
            end
        end
        
        function pathObject = getSelectedObject()
            hierarchy = QuPath.getHierarchy();
            if isempty(hierarchy)
                pathObject = [];
            else
                pathObject = hierarchy.getSelectionModel().getSelectedObject();
            end
        end
        
        function roi = getSelectedROI()
            pathObject = QuPath.getSelectedObject();
            if isempty(pathObject)
                roi = [];
            else
                roi = pathObject.getROI();
            end
        end
        
        function roi = createRectangle(x, y, w, h)
            roi = qupath.lib.roi.RectangleROI(x, y, w, h);
        end
        
        function roi = createEllipse(x, y, w, h)
            roi = qupath.lib.roi.EllipseROI(x, y, w, h);
        end
        
        function roi = createPolygon(x, y)
            roi = qupath.lib.roi.PolygonROI(x, y, -1, 0, 0);
        end
        
        function roi = createLine(x, y, x2, y2)
            roi = qupath.lib.roi.LineROI(x, y, x2, y2, -1, 0, 0);
        end
        
        function pathObject = createAnnotationObject(roi, pathClass)
            pathObject = qupath.lib.objects.PathAnnotationObject(roi);
            if nargin > 1 && ~isempty(pathClass)
                pathObject.setPathClass(pathClass);
            end
        end
        
        function pathObject = createDetectionObject(roi, pathClass)
            pathObject = qupath.lib.objects.PathDetectionObject(roi);
            if nargin > 1 && ~isempty(pathClass)
                pathObject.setPathClass(pathClass);
            end
        end
        
        function pathObject = addRectangleAnnotation(x, y, w, h, doSelect)
            hierarchy = QuPath.getHierarchy();
            if isempty(hierarchy)
                error('No hierarchy available!');
            else
                roi = QuPath.createRectangle(x, y, w, h);
                pathObject = QuPath.createAnnotationObject(roi);
                hierarchy.addPathObject(pathObject, false);
                if nargin < 5 || doSelect
                    hierarchy.getSelectionModel().setSelectedObject(pathObject);
                end
            end
        end
        
        function hierarchy = addObject(pathObject)
            hierarchy = QuPath.getHierarchy();
            if isempty(hierarchy)
                error('No hierarchy available!');
            else
                hierarchy.addPathObject(pathObject, false);
            end
        end
        
        function server = getServer()
            imageData = QuPath.getImageData();
            if isempty(imageData)
                server = [];
            else
                server = imageData.getServer();
            end
        end
        
        function path = getImagePath()
            server = QuPath.getServer();
            if isempty(server)
                path = [];
            else
                path = server.getPath();
            end
        end
        
        function w = getImageWidth()
            server = QuPath.getServer();
            if isempty(server)
                w = [];
            else
                w = server.getWidth();
            end
        end
        
        function h = getImageHeight()
            server = QuPath.getServer();
            if isempty(server)
                h = [];
            else
                h = server.getHeight();
            end
        end
        
        function tmaGrid = getTMAGrid()
            hierarchy = QuPath.getHierarchy();
            if isempty(hierarchy)
                tmaGrid = [];
            else
                tmaGrid = hierarchy.getTMAGrid();
            end
        end
        
        function cores = getTMACores()
            tmaGrid = QuPath.getTMAGrid();
            if isempty(tmaGrid)
                cores = [];
            else
                cores = cell(tmaGrid.getTMACoreList().toArray());
            end
        end
        
        function im = getImage(roi, downsample)
            server = QuPath.getServer();
            if isempty(server)
                error('No image server available!');
            end
            path = server.getPath();
            region = qupath.lib.regions.RegionRequest.createInstance(path, downsample, roi);
            img = server.readBufferedImage(region);
            
            % Convert BufferedImage into something more MATLAB-friendly
            % (Currently, this only handles 8-bit images)
            h = img.getHeight();
            w = img.getWidth();
            d = img.getData().getNumBands();
            if server.isRGB()
                type = 'uint8';
            else
                type = 'single';
            end
            im = zeros([h, w, d], type);
            for i = 1:d
                px = img.getData().getSamples(0, 0, w, h, i-1, zeros(0, type));
                im(:, :, i) = reshape(px, w, h)';
            end
        end
        
        function detections = addDetections(parentObject, L)
            % Get the ROI for the image
            roi = parentObject.getROI();
            xBounds = roi.getBoundsX();
            yBounds = roi.getBoundsY();
            
            % Estimate the downsample from the image size
            downsampleX = roi.getBoundsWidth() / size(L, 2);
            downsampleY = roi.getBoundsHeight() / size(L, 1);
            downsample = (downsampleX + downsampleY)/2;
            % Warn if the x & y downsamples are suspiciously different
            if abs(downsampleX - downsampleY)/downsampleX > 0.001
                warning('Downsample values %.3f and %.3f do not match!', downsampleX, downsampleY);
            end
            
            % Create a useful structure
            input = create_region_struct(downsample, xBounds, yBounds);
            
            % Convert to objects
            path_objects = labels2objects(L, input, false);
            
            % Create detections
            detections = java.util.ArrayList();
            for ii = 1:numel(path_objects)
                path_object = path_objects(ii);
                poly = QuPath.createPolygon(path_object.x, path_object.y);
                detections.add(QuPath.createDetectionObject(poly));
            end
            
            % Add to image
            parentObject.addPathObjects(detections);
            hierarchy = QuPath.getHierarchy();
            hierarchy.fireHierarchyChangedEvent('MATLAB', parentObject);
            
        end
        
    end
end