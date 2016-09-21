class atg ( $atg_archive , $atg_install_config_file, $atg_folder , $atg_install_dir , $atg_bin_file , $atg_source_archive, $user, $group, $logoutput) {

    $atg_home = "${atg_install_dir}/${atg_folder}"
    $temp_directory = hiera('temp_directory')    
    $java_home="/opt/oracle/product/java/jdk1.8.0_45/bin"
 
    require common, java    
    Exec {
        path => [ "${java_home}", "/usr/bin", "/bin", "/usr/sbin", "${temp_directory}"]
    }
    notify{"The value is java_home-: ${java_home}": }
    notify{"The value is atg_home-: ${atg_home}": }
    notify{"The value is atg_installed-: ${atg_installed}": }
    notify{"The value is atg_folder-: ${atg_folder}": }
    notify{"The value is atg_install_dir-: ${atg_install_dir}": }
    notify{"The value is atg_install_config_file-: ${atg_install_config_file}": }

       if ( $atg_installed == false ) {    
            exec { 'extract atg zip':
            cwd     => "${temp_directory}",
            command => "unzip -o ${atg_source_archive}",
            creates => "${atg_home}",
        }
        ->        
        file { "fix atg bin permissions" :
            ensure => "present",
            path   => "${temp_directory}/${atg_bin_file}",
            owner  => "${user}",
            mode   => "0755"
        }
        ->
        file { "${temp_directory}/${atg_install_config_file}" :
            owner   => "${user}",
            mode    => "0755",
            content => template("${module_name}/${atg_install_config_file}.erb")
        }
        -> 
        file { 'atg install folder' :
            path   => "${atg_install_dir}",
            ensure => directory,
            owner  => "${user}"
        }
        ->
        exec { 'execute atg bin':
            cwd     => "${temp_directory}",
            timeout => 0,
            command => "${temp_directory}/./${atg_bin_file} -f ${temp_directory}/${atg_install_config_file} -i silent",
            logoutput => "${logoutput}",
            creates => "${atg_home}"
        }
        ->
        exec { 'fix atg permissions':
            cwd     => "${temp_directory}",
            timeout => 0,
            command => "chown -R ${user}:${group} ${atg_install_dir}"
        }
        -> 
        exec { 'cleanup atg temp':
            cwd     => "${temp_directory}",
            command => "rm -rf ${temp_directory}/${atg_archive} ${temp_directory}/${atg_bin_file} ${temp_directory}/${atg_install_config_file}"
        }
        ->
        file { "/etc/profile.d/atg.sh":
          content => "export DYNAMO_ROOT=${atg_home} \nexport DYNAMO_HOME=${atg_home}/home \nexport PATH=\$PATH:\$DYNAMO_HOME/bin \n"
        }
           
    }
}
