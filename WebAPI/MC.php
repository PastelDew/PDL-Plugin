<?php
defined('BASEPATH') OR exit('No direct script access allowed');

use Symfony\Component\Yaml\Yaml;

class MC extends CI_Controller {
    public function __construct(){
        parent::__construct();
        $this->version = "0.0.1";
        $this->root_dir = "/host/home2/pdlab/storage/MC/";
    }

	public function index()
	{
        $this->load_failed();
    }

    public function plugins()
    {
        $this->load_success($this->get_plugins());
    }

    public function update_check($label, $version, $coreVersion, $check_index = 0){
        $plugins = $this->get_plugins();
        $ver_t = $this->extract_version($version);
        $updatable = array(
            "updatable" => FALSE,
            "updateVersion" => $version,
            "critical" => FALSE,
            "coreUpdateRequired" => FALSE,
            "coreUpdateVersion" => ""
        );
        foreach($plugins as $plugin){
            if($plugin["label"] != $label) continue;
            $ver_s = $this->extract_version($plugin["latestVersion"]);
            
            if($this->is_updatable($ver_t, $ver_s, $check_index)){
                $updatable["updatable"] = TRUE;
                $updatable["updateVersion"] = join(".", $ver_s);
            }

            $info = $this->read_label_info($label);
            if(array_key_exists($version, $info["critical_update"])){
                $updatable["updatable"] = TRUE;
                $updatable["updateVersion"] = $info["critical_update"][$version];
                $updatable["critical"] = TRUE;
            }

            if($label == "pdl")
                break;

            if($updatable["updatable"]){
                $p = $this->get_plugin_yml($label, $updatable["updateVersion"]);
                //$p = get_object_vars($p);
                $reqCoreVer = array_key_exists("requiredCoreVersion", $p) ? $p["requiredCoreVersion"] : null;
                $coreUpdatable = FALSE;
                if($reqCoreVer) {
                    $cv = $this->extract_version($coreVersion);
                    $rv = $this->extract_version($reqCoreVer);
                    $coreUpdatable = $this->is_updatable($cv, $rv);
                    if($updatable["critical"] && $coreUpdatable){
                        $updatable["coreUpdateRequired"] = TRUE;
                        $updatable["coreUpdateVersion"] = $reqCoreVer;
                    }
                }

                if(!$updatable["critical"] && $coreUpdatable){
                    $curver = $this->extract_version($version);
                    rsort($plugin["versions"]);
                    $foundVersion = FALSE;
                    foreach($plugin["versions"] as $plver){
                        $pv = $this->extract_version($plver);
                        if(!$this->is_updatable($curver, $pv, $check_index)) continue;

                        $p = $this->get_plugin_yml($label, $plver);
                        //$p = get_object_vars($p);
                        $reqCoreVer = array_key_exists("requiredCoreVersion", $p) ? $p["requiredCoreVersion"] : null;
                        if($reqCoreVer) {
                            $cv = $this->extract_version($coreVersion);
                            $rv = $this->extract_version($reqCoreVer);
                            $coreUpdatable = $this->is_updatable($cv, $rv);
                            if($coreUpdatable) continue;
                        }
                        $updatable["updateVersion"] = $plver;
                        $foundVersion = TRUE;
                        break;
                    }
                    if(!$foundVersion){
                        $updatable = array(
                            "updatable" => FALSE,
                            "updateVersion" => $version,
                            "critical" => FALSE,
                            "coreUpdateRequired" => FALSE,
                            "coreUpdateVersion" => ""
                        );
                    }
                }
            }
            break;
        }
        $this->load_success($updatable);
    }

    public function download($label, $version){
        $dir_plugin = join("/", [
            $this->root_dir,
            $label,
            $version
        ]);
        
        $files = $this->get_file_list($dir_plugin);
        if(count($files)){
            $filename = $files[0];
            $this->push_file("$dir_plugin/$filename", $filename);
        }
    }

    public function plugin_info($label, $version)
    {
        $this->load_success($this->get_plugin_yml($label, $version));
    }

    private function get_plugins(){
        $dirs = $this->get_dir_list($this->root_dir);
        $plugins = array();
        foreach($dirs as $d){
            $versions = $this->get_dir_list($this->root_dir . "/" . $d);
            $files = array();
            foreach($versions as $v){
                $filenames = $this->get_file_list("$this->root_dir/$d/$v");
                if(!count($filenames))
                    continue;
                $files[] = $filenames[0];
            }
            $plugins[] = array(
                "label" => $d,
                "latestVersion" => $versions[count($versions) - 1],
                "firstVersion" => $versions[0],
                "versions" => $versions,
                "filenames" => $files
            );
        }
        return $plugins;
    }

    private function get_plugin_yml($label, $version){
        $dirs = $this->get_dir_list($this->root_dir);
        $plugins = array();
        $foundDir = null;
        foreach($dirs as $d){
            if($d != $label) continue;
            $foundDir = $d;
        }
        if(!$foundDir) return array();
        $versions = $this->get_dir_list($this->root_dir . "/" . $label);
        $files = array();
        $foundVersion = null;
        foreach($versions as $v){
            if($v == $version){
                $foundVersion = $v;
                break;
            }
        }
        if($foundVersion == null) return array();
        $filenames = $this->get_file_list("$this->root_dir/$label/$version");
        if(!count($filenames)) return array();

        $jarName = $filenames[0];
        
        //$result = exec("java -jar JarDependencyChecker.jar $label $version $jarName");
        $result = $this->readJar($label, $version, $jarName);
        if(!count($result)) return array();
        return $result;
    }

    private function readJar($label, $version, $jarName){
        chdir($this->root_dir);
        $serviceEntry = $this->readZipFileEntry($label . "/$version/$jarName", "service.yml");
        if(!$serviceEntry){
            $serviceEntry = $this->readZipFileEntry($label . "/$version/$jarName", "plugin.yml");
            if(!$serviceEntry) return array();
        }
        return Yaml::parse($serviceEntry);
    }

    private function readZipFileEntry($zipFileName, $searchEntryName) {
        $zip = zip_open($zipFileName);
     
        if ($zip) {
            while ($zipEntry = zip_read($zip)) {
                $entryName = zip_entry_name($zipEntry);
                
                if ($entryName == $searchEntryName) {
                    if (zip_entry_open($zip, $zipEntry, "r")) {
                        $searchFileContents = zip_entry_read($zipEntry, zip_entry_filesize($zipEntry));
     
                        zip_entry_close($zipEntry);
                        zip_close($zip);
                        
                        return $searchFileContents;
                    }			
                }
            }
     
            zip_close($zip);
        }
        
        return false;
    }

    private function read_label_info($label){
        $filename = "$this->root_dir/$label/info.json";
        if(!file_exists($filename)){
            $f = fopen($filename, "w");
            fwrite($f, json_encode(array(
                "author" => "unknown",
                "created" => date('Y-m-d h:i:s', time()),
                "critical_update" => new stdClass
            )));
            fclose($f);
        }
        $string = file_get_contents($filename);
        return json_decode($string, true);
    }

    private function is_updatable($version_t, $version_s, $index = 0){
        assert(is_array($version_t) && is_array($version_s), "Assertion Failed.");

        $cnt_t = count($version_t);
        $cnt_s = count($version_s);
        for(;$index < $cnt_t && $index < $cnt_s; $index++){
            $vt = $version_t[$index];
            $vs = $version_s[$index];
            if($vt < $vs)
                return TRUE;
            else if($vt > $vs)
                return FALSE;
        }
        if($cnt_t > $cnt_s) return TRUE;
        return FALSE;
    }

    private function extract_version($version){
        $ver = explode(".", $version);
        $versions = array();
        foreach($ver as $v)
            $versions[] = (int) $v;
        
        return $versions;
    }

    private function load_success($data){
        $this->load->view("MC/Response", array(
            'apiVersion' => $this->version,
            'errorCode' => 0,
            'errorMessage' => "",
            'data' => json_encode($data)
		));
    }

    private function load_failed($errorCode = -1, $errorMessage = "Invalid request.", $response = null){
        $this->load->view("MC/Response", array(
            'apiVersion' => $this->version,
            'errorCode' => $errorCode,
            'errorMessage' => $errorMessage,
            "data" => $response
		));
    }

    private function get_dir_list($path)
    {
        $handle = opendir($path);
        $files = array();

        while (false !== ($filename = readdir($handle))) {
            $newPath = $path . "/" . $filename;
            if(is_file($newPath) || $filename == "." || $filename == ".."){
                continue;
            }

            $files[] = $filename;
        }
        closedir($handle);
        sort($files);
        return $files;
    }

    private function get_subdir_list($path)
    {
        $handle = opendir($path);
        $files = array();

        while (false !== ($filename = readdir($handle))) {
            $newPath = $path . "/" . $filename;
            if(is_file($newPath) || $filename == "." || $filename == ".."){
                continue;
            }

            $files[] = $filename;
            $subFiles = $this->get_dir_list($newPath);
            foreach($subFiles as $f){
                $files[] = $filename . "/" . $f;
            }
        }
        closedir($handle);
        sort($files);
        return $files;
    }

    private function get_file_list($path)
    {
        $handle = opendir($path);
        if(!isset($handle)) return array();
        $files = array();

        while (false !== ($filename = readdir($handle))) {
            $newPath = $path . "/" . $filename;
            if(!is_file($newPath))
                continue;

            $files[] = $filename;
        }
        closedir($handle);
        sort($files);
        return $files;
    }

    private function push_file($path, $name)
	{
		// make sure it's a file before doing anything!
		if(is_file($path))
		{
			// required for IE
			if(ini_get('zlib.output_compression')) { ini_set('zlib.output_compression', 'Off'); }

			// get the file mime type using the file extension
			$this->load->helper('file');

			$mime = get_mime_by_extension($path);

			// Build the headers to push out the file properly.
			header('Pragma: public');     // required
			header('Expires: 0');         // no cache
			header('Cache-Control: must-revalidate, post-check=0, pre-check=0');
			header('Last-Modified: '.gmdate ('D, d M Y H:i:s', filemtime ($path)).' GMT');
			header('Cache-Control: private',false);
			header('Content-Type: '.$mime);  // Add the mime type from Code igniter.
			header('Content-Disposition: attachment; filename="'.basename($name).'"');  // Add the file name
			header('Content-Transfer-Encoding: binary');
			header('Content-Length: '.filesize($path)); // provide file size
			header('Connection: close');
			readfile($path); // push it out
		}
	}
}
