# TriMO-EMS
Use the master branch.

The main class file (executed class) is TopLevelEnergyDistr.

To execute the Java code, first compile it, which should result in .class binaries generated under default path "\<local project repo path\>\out\production\JMETALHOME\".

Another requirement is that you have your dataset files ready under path "\<local project repo\>\data\".
Three dataset files are required: APP_ENERGY, APP_PREFERENCES and RENEWABLE, as mentioned in the IEEE DataPort dataset repository:

https://ieee-dataport.org/documents/household-appliance-usage-preferences-appliance-energy-consumption-and-hourly-renewable

Please note that this implementation merges the PARAMS_VALUES dataset file into the beginning of the APP_ENERGY dataset file, i.e., the first 3 lines in the APP_ENERGY dataset file should consist of the 3 mandatory lines of the PARAMS_VALUES dataset file, followed by an empty line.

Finally, run the following command in the cmd terminal, having the appropriate Java version installed (e.g., jdk1.8.0_221) and (in addition) the gson JAR/library (e.g., gson-2.10.1.jar):

"C:\Program Files\Java\jdk1.8.0_221\bin\java.exe" -Xms8192m -Xmx12288m -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.8.0_221\jre\lib\\*;C:\Users\\<local project repo path\>\out\production\JMETALHOME;C:\Users\\<local project repo path\>\gson-2.10.1.jar" jmetal.metaheuristics.trilevel.TopLevelEnergyDistr \<APP_ENERGY dataset filename\> \<APP_PREFERENCES dataset filename\> \<RENEWABLE dataset filename\> MOEAD - C:\Users\\<local project repo path\>\ -

Alternatively, you may execute the Java code in the IDE of your choice. Mine is IntelliJ.
