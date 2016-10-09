function [path_qupath, path_classpath, path_library_path] = setup_qupath(path_qupath)
% Setup MATLAB so that QuPath can be launched directly from the command
% line.
% 
% INPUT:
%    PATH_QUPATH - Path to QuPath installation (optional).
%                  If not provided, a prompt will be shown to select the
%                  installation instead.
% 
% OUTPUT:
%    PATH_QUPATH - Path to QuPath installation - either passed as input, or
%                  selected from prompt.
%    PATH_CLASSPATH - Path to text file containing MATLAB's static Java
%                     classpath.
%    PATH_LIBRARY_PATH - Path to text file containing MATLAB's Java
%                        library path.
% 
% Author: Pete Bankhead, 2016


% Check Java version
v = version('-java');
fprintf('------------------------------------------------\n');
fprintf('CHECKING JAVA RUNTIME ENVIRONMENT\n');
if isempty(strfind(v, 'Java 1.8'))
    warning('Java 1.8 required for QuPath!\nCurrent version: %s\nYou will need to install Java 8 JDK and change the MATLAB Java Runtime Environment for QuPath to launch correctly.\nSee web link for more details.', v);
    if ispc
        web('http://uk.mathworks.com/matlabcentral/answers/130359-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-on-windows');
    elseif ismac
        web('http://uk.mathworks.com/matlabcentral/answers/103056-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-for-mac-os');
    else
        web('http://uk.mathworks.com/matlabcentral/answers/130360-how-do-i-change-the-java-virtual-machine-jvm-that-matlab-is-using-for-linux-os');
    end
else
    fprintf('QuPath requires Java 1.8\nSeems ok... Current version %s...\n', v);
end
fprintf('------------------------------------------------\n');


% Check if classpath files exist
fprintf('CHECKING JAVA RUNTIME ENVIRONMENT\n');
path_classpath = fullfile(prefdir, 'javaclasspath.txt');
path_library_path = fullfile(prefdir, 'javalibrarypath.txt');
if exist(path_classpath, 'file')
    warning('%s exists - will be overwritten', path_classpath);
else
    fprintf('%s does not exist - will be created\n', path_classpath);
end
if exist(path_library_path, 'file')
    warning('%s exists - will be overwritten', path_library_path);
else
    fprintf('%s does not exist - will be created\n', path_library_path);
end
fprintf('------------------------------------------------\n');

% Request QuPath path
fprintf('CHECKING QUPATH INSTALLATION\n');
if nargin < 1 || isempty(path_qupath)
    fprintf('Please select QuPath installation directory...\n');
    if ismac
        [fn, pn] = uigetfile('QuPath.app', 'Find location of QuPath.app');
        path_qupath = fullfile(pn, fn, 'Contents');
    else
        path_qupath = uigetdir([], 'Find QuPath installation directory');
        % TODO: Correct directory as required on non-OSX platforms
    end
end
if ~isdir(path_qupath)
    warn('Unknown QuPath installation directory %s', path_qupath);
    return;
end

% Recursively search for jars - but also make sure not to
% go into Java.runtime (or other such bad places...)
jar_paths = get_jar_paths(path_qupath, {});
% Look for JavaFX Runtime... appears to be required (?)
% First, check we don't already have it
path_jfxrt = [];
for path = javaclasspath
    [~, name] = fileparts(path);
    if isequal(name, 'jfxrt.jar')
        path_jfxrt = path;
        break
    end
end
if ~isempty(path_jfxrt)
    % Note: we've found the JavaFX RunTime, but will continue to add it
    % anyway... the reason being that maybe we added it ourselves
    % previously
    % (More checks could be done in this regard, but haven't been yet...)
    fprintf('JavaFX Runtime already on classpath: %s\n', path_jfxrt);
end
path_jfxrt = recursive_file_search(path_qupath, 'jfxrt.jar');
if exist(path_jfxrt, 'file')
    fprintf('Found JavaFX Runtime: %s\n', path_jfxrt);
    jar_paths = cat(1, jar_paths, path_jfxrt);
else
    warning('Could not find jfxrt.jar - if QuPath doens''t load, try looking for this and adding it to the dynamic or static path');
end

% Add Jar paths to static classpath
fid = fopen(path_classpath, 'w');
dir_main = [];
for jj = 1:numel(jar_paths)
    jp = jar_paths{jj};
    fprintf(fid, '%s\n', jp);
    % While doing this, get the directory containing QuPathApp.jar -
    % that's the main directory, which needs to be set as the library path    
    if isempty(dir_main)
        [path_dir, name] = fileparts(jp);
        disp(name)
        if isequal(name, 'QuPathApp')
            dir_main = path_dir;
        end
    end
end
fclose(fid);
fprintf('Written %d jar paths to %s\n', numel(jar_paths), path_classpath);

% Add native library directory
if isdir(dir_main)
    fid = fopen(path_library_path, 'w');
    fprintf(fid, dir_main);
    fclose(fid);
    fprintf('Written main directory %s to %s\n', dir_main, path_library_path);
else
    warning('Could not find main directory containing native libraries');
end

% Try adding directory of this m-file to MATLAB path
d = fileparts(mfilename('fullpath'));
addpath(d);
savepath
fprintf('Saved %s to MATLAB path\n', d);



    % Get paths to all jar files within specified (QuPath) directory,
    % looking recursively while trying to skip the JRE itself
    function jars = get_jar_paths(d, jars)
        % Get Jar paths from a base directory
        if ~isdir(d)
            return
        end
        % Try to avoid the JRE...
        [~, d_name] = fileparts(d);
        if isequal(d_name, 'PlugIns')
            return;
        end
        % Add jars & continue search recursively
        files = dir(d);
        for ii = 1:numel(files)
            f = files(ii);
            if isequal(f.name(1), '.')
                continue;
            end
            filename = fullfile(d, f.name);
            if f.isdir
                jars = get_jar_paths(filename, jars);
            else
                [~, ~, ext] = fileparts(lower(filename));
                if isequal(ext, '.jar')
                    jars = cat(1, jars, filename);
                end
            end
        end
    end

    % Recursively search for a file with a specified name
    function path = recursive_file_search(d, search_name)
        path = [];
        % Get Jar paths from a base directory
        if ~isdir(d)
            return
        end
        % Continue search recursively
        files = dir(d);
        for ii = 1:numel(files)
            f = files(ii);
            if isequal(f.name(1), '.')
                continue;
            elseif isequal(f.name, search_name)
                path = fullfile(d, f.name);
                return;
            end
            filename = fullfile(d, f.name);
            if f.isdir
                path = recursive_file_search(filename, search_name);
                if ~isempty(path)
                    return;
                end
            end
        end
    end


end