# 6 args
#====================
# 0 => file name(unused arg)
# 1 => user_group name "ai_usr" 
# 2 => application_path "/va/www/app_path"
# 3 => domain name "www.demo.com"
# 4 => reverse_proxy_port server domain port "9089"
# 5 => other_bin_path ":/opt/jdk-12.0.1/bin" if application has java or other bin depandancy

# example :./file.sh user_group="ai_usr" application_path="/var/www/mywizard-pythonapps/automation" domain="hello-demo.com.hello" reverse_proxy_port="8090" other_bin_path=":/opt/jdk-12.0.1/bin"

reverse_proxy_port=''
domain=''
application_path=''
user_group=''
config_file_name=''
other_bin_path=''
sh_execute_type=''
profile_type=''

for ARGUMENT in "$@"
do

    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)

    case "$KEY" in

                reverse_proxy_port)						reverse_proxy_port=${VALUE};;
                domain)									domain=${VALUE} ;;
                application_path)						application_path=${VALUE};;
                user_group)								user_group=${VALUE};;
				config_file_name)						config_file_name=${VALUE};;
				other_bin_path)							other_bin_path=${VALUE};;
				sh_execute_type)						sh_execute_type=${VALUE};;
				profile_type)							profile_type=${VALUE};;
            *)
    esac


done

if [ "$reverse_proxy_port" = '' ]|| [ "$domain" = '' ] || [ "$application_path" = '' ]|| [ "$user_group" = '' ]; then
    echo
    echo "Following args are mandatory: reverse_proxy_port, domain, application_path, user_group"
	echo
	echo "kindly validate args"
	exit 1
fi


if [ "$profile_type" = '' ]; then

	webapp_FILE=webapp.ini

	if [ -f ${webapp_FILE} ] && [ "${sh_execute_type}"='' ];then
		echo "ini, ${webapp_FILE} exist"
	else
		echo "[uwsgi]
			  http = 0.0.0.0:${reverse_proxy_port}
			  wsgi-file = app_wsgi.py
			  processes = 2
			  threads = 1
              logto = /uwsgi.log
              buffer-size = 8192
			  logformat = [%(ctime)] { %(pid) } { %(uri) | %(method) | %(status) } { resp_time: %(msecs)ms | resp_hdr_size: %(hsize) | resp_body_size: %(rsize)
              log-maxsize= 10000000
              logfile-chmod = 666" > webapp.ini
	fi
fi

service_FILE="/etc/systemd/system/"${domain}.service

if [ -f ${service_FILE} ] && [ "${sh_execute_type}"='' ];then
    echo "service, ${service_FILE} exist"

	#service restart 
	sudo systemctl restart ${domain}.service
	#service status
	sudo systemctl status ${domain}.service
else
	echo "[Unit]
	Description=uWSGI instance to serve ${domain}
	After=network.target

	[Service]
	User=${user_group}
	Group=${user_group}
	WorkingDirectory=${application_path}
	Environment=\"PATH=${application_path}/venv/bin${other_bin_path}\"
	ExecStart=${application_path}/uwsgi --http :9038 --module app_wsgi:application --processes 4 --threads 2
 

	[Install]
	WantedBy=multi-user.target"  > ${domain}.service
	#service
	(cp -R ${domain}.service /etc/systemd/system)

	#service start 
	sudo systemctl start ${domain}.service

	#service enabled 
	sudo systemctl enable ${domain}.service

	#service restart 
	sudo systemctl restart ${domain}.service

	#service status
	sudo systemctl status ${domain}.service
fi
