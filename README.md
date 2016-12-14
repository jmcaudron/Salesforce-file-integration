# Build Metadata tool

To build  tool:

    mvn clean package 
    
The build will include the appropriate eclipse swt jar by detecting the operating system type.  If you would like to manually specify the eclipse swt jar, take a look at the pom.xml file to see a full list of available profiles.

    
# Execute load tool 


    
To run tool from the command line with mvn, use the  command:

    mvn exec:java -Dexec.mainClass=com.jmc.force.bulk.loadFile
    
To run tool from the command line with java, use the  command: 
    
    java -cp target/sforceFileIntegration-0.0.2-shade.jar com.jmc.force.bulk.LoadFile
    
or
    
    java -jar target/sforceFileIntegration-0.0.2-shade.jar


