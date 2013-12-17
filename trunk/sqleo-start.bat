:: use :: on begining of line to comment
:: This bat file for windows OS only runs SQLeo app with specified look and feel
:: (personally nimbus one is looking cool)
:: For linux users - rename this file extension to "sh" 
:: For MacOSX users - rename this file extension to "command"
:: Note - Java should be already in your PATH variable

java -Dcom.sqleo.laf.class=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel -jar SQLeoVQB.jar

:: Other available look and feels from jdk,replace the class name below mentioned in above command 
:: javax.swing.plaf.metal.MetalLookAndFeel (this is default one)
:: com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel
:: com.sun.java.swing.plaf.windows.WindowsLookAndFeel(Windows)
:: com.sun.java.swing.plaf.motif.MotifLookAndFeel
:: com.sun.java.swing.plaf.gtk.GTKLookAndFeel (For some linux variants like Ubuntu)
:: com.apple.laf.AquaLookAndFeel(MacOS)

:: This one is external look and feel similar to Nimbus but more MacOS styled  
:: download the seaglasslookandfeel-0.2.jar from http://seaglass.googlecode.com/svn/doc/downloads.html
:: and place it just beside sqleo jar and use following command to launch SQLeo
:: On the Mac as in Linux you need to separate classpaths with : instead of ; iin below command
:: Note seaglasslookandfeel is not working well with sqleo

:: java -classpath .;SQLeoVQB.jar;seaglasslookandfeel-0.2.jar -Dcom.sqleo.laf.class=com.seaglasslookandfeel.SeaGlassLookAndFeel com.sqleo.environment.Application


