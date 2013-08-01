!/bin/bash

# Check if jdk8.gz file is present
if [ -f /PATH/TO/TMP_FOLDER/jdk8.gz ]
then
  # Remove installed jdk8
  sudo rm -r /opt/jdk1.8.0
  
  # Extract new jdk8 and remove jdk8.gz file after extraction
  sudo tar zxvf /PATH/TO/TMP_FOLDER/jdk8.gz -C /opt && rm /PATH/TO/TMP_FOLDER/jdk8.gz  
fi

# Start the program
/opt/jdk1.8.0/bin/java -cp /PATH/TO/LIBS/LIB1.jar:/PATH/TO/LIBS/LIB2.jar:/PATH/TO/APPLICATION/APPLICATION.jar PACKAGE.MAIN_CLASS
