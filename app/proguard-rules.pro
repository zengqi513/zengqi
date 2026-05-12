# Add project specific ProGuard rules here.

# Room
-keep class com.autobookkeeper.data.** { *; }
-keepclassmembers class com.autobookkeeper.data.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Apache POI
-keep class org.apache.poi.** { *; }
-keepclassmembers class org.apache.poi.** { *; }
-keep class org.apache.commons.** { *; }
-keepclassmembers class org.apache.commons.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**

# Keep all Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Saxon/XPath (Apache XMLBeans)
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.xmlbeans.impl.xpath.saxon.**

# Log4j OSGi
-dontwarn org.apache.logging.log4j.util.OsgiServiceLocator

# Auto-generated missing rules
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn com.gemalto.jp2.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
