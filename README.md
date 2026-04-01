# TriMO-EMS
Use the master branch.
The main class file (executed class) is TopLevelEnergyDistr.
To execute the Java code, first compile it, which should result in .class binaries generated under default path "\<local project repo path\>\out\production\JMETALHOME".
Then, run the following command in the cmd terminal, having the appropriate Java version installed (e.g., jdk1.8.0_221) and (in addition) the gson JAR/library (e.g., gson-2.10.1.jar):

"C:\Program Files\Java\jdk1.8.0_221\bin\java.exe" -Xms8192m -Xmx12288m -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.8.0_221\jre\lib*;C:\Users\<local project repo path\>\out\production\JMETALHOME;C:\Users\<local project repo path\>\gson-2.10.1.jar" jmetal.metaheuristics.trilevel.TopLevelEnergyDistr MOEAD - C:\Users\<local project repo path\>\ -

Alternatively, you may execute the Java code in the IDE of your choice. Mine is IntelliJ.
