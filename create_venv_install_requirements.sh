echo
echo    GenAI Python Chatbot
echo

#this part added for get service name and ENV profile
# EX:./file.sh profile="dev"
profile=''
profile_type=''
service_name=''
for ARGUMENT in "$@"
do
    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)
 
    case "$KEY" in
 
                service_name)						service_name=${VALUE};;
                profile)							profile=${VALUE} ;;
                profile_type)						profile_type=${VALUE} ;;
 
            *)
    esac
done
 
 
if [ -z "$APP_HOME_GENWIZARDCHATBOT_PYTHON" ]; then
    echo
    echo "  Setting APP_HOME_GENWIZARDCHATBOT_PYTHON to current dir, which is: $PWD"
    APP_HOME_GENWIZARDCHATBOT_PYTHON=$PWD
else
    echo 
    echo "  APP_HOME_GENWIZARDCHATBOT_PYTHON is set to $APP_HOME_GENWIZARDCHATBOT_PYTHON"
fi

create_venv="/usr/local/bin/python3.13 -m venv $APP_HOME_GENWIZARDCHATBOT_PYTHON/venv"
activate_venv="source $APP_HOME_GENWIZARDCHATBOT_PYTHON/venv/bin/activate"
#upgrade_pip="$APP_HOME_GENWIZARDCHATBOT_PYTHON/venv/bin/python3.12 -m pip install $APP_HOME_GENWIZARDCHATBOT_PYTHON/pip/*.whl"
install_app_req="$APP_HOME_GENWIZARDCHATBOT_PYTHON/venv/bin/python3.13 -m pip install -r $APP_HOME_GENWIZARDCHATBOT_PYTHON/requirements_offline_linux"
 
## delete existing 'venv' dir
venv_path="$APP_HOME_GENWIZARDCHATBOT_PYTHON/venv"
if [ -d $venv_path ]; then
    echo
    echo "      Deleting existing 'venv'"
    rm -rf $APP_HOME_GENWIZARDCHATBOT_PYTHON/venv
    echo "      Done"
fi
 
 
echo
echo "  Creating python virtual env"
$create_venv
 
echo
echo "  Activating python virtual env"
$activate_venv
 
# echo 
# echo "  Upgrading 'pip'"
# $upgrade_pip
 
 
# echo "Copy wheel file names to requirements_offline_linux"
ls -d $APP_HOME_GENWIZARDCHATBOT_PYTHON/offfline_packages_linux/* > requirements_offline_linux
 
echo 
echo "  Installing dependencies"
$install_app_req
 
 
if [ "$profile_type" != '' ] && [ "$profile" != '' ]; then
 
    echo "  copying app_config.ini from $APP_HOME_GENWIZARDCHATBOT_PYTHON to $APP_HOME_GENWIZARDCHATBOT_PYTHON
   sudo cp $APP_HOME_GENWIZARDCHATBOT_PYTHON/profile/${profile}/app_config.ini $APP_HOME_GENWIZARDCHATBOT_PYTHON
    echo "  copying of app_config.ini completed"
    
    echo "  copying webapp.ini from $APP_HOME_GENWIZARDCHATBOT_PYTHON to $APP_HOME_GENWIZARDCHATBOT_PYTHON"
   sudo cp $APP_HOME_GENWIZARDCHATBOT_PYTHON/profile/${profile}/webapp.ini $APP_HOME_GENWIZARDCHATBOT_PYTHON
    echo "  copying of webapp.ini completed"
 
    echo "  copying app.json from $APP_HOME_GENWIZARDCHATBOT_PYTHON to $APP_HOME_GENWIZARDCHATBOT_PYTHON
    sudo cp $APP_HOME_GENWIZARDCHATBOT_PYTHON/app.json $APP_HOME_GENWIZARDCHATBOT_PYTHON
    echo "  copying of app.json completed"
else
 
    echo "  copying app_config.ini from $APP_HOME_GENWIZARDCHATBOT_PYTHON to $APP_HOME_GENWIZARDCHATBOT_PYTHON
    echo "  copying of app_config.ini completed"
 
    echo "  copying app.json from $APP_HOME_GENWIZARDCHATBOT_PYTHON to $APP_HOME_GENWIZARDCHATBOT_PYTHON
    cp $APP_HOME_GENWIZARDCHATBOT_PYTHON/app.json $APP_HOME_GENWIZARDCHATBOT_PYTHON/
    echo "  copying of app.json completed"
fi
 
APP_NAME=$(basename $APP_HOME_GENWIZARDCHATBOT_PYTHON)
APP_LOG_DIR=/var/www/Logs/$APP_NAME
echo "Creating 'LOG' dir & granting W permissions to all"
 
mkdir -p $APP_LOG_DIR
 
chmod 777 -R $APP_LOG_DIR
 
rm -r $APP_HOME_GENWIZARDCHATBOT_PYTHON/profile
echo "profile folder deleted sucessfully"
echo "end"