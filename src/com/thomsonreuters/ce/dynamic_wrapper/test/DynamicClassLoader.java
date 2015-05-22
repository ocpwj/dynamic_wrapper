package com.thomsonreuters.ce.dynamic_wrapper.test;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;
import java.util.Vector;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class DynamicClassLoader extends ClassLoader {
	
	
	@Override
	public URL getResource(String ResourceName) {
		System.out.print("Looking for resource:"+ResourceName);
		// TODO Auto-generated method stub
		for (String thisPath : ClassPath)
		{
			try {
				URL RescourceURL;
				if (thisPath.endsWith(".jar")|| thisPath.endsWith(".zip"))
				{
					//get class byte code from jar file
					JarFile jarFile = new JarFile(thisPath);
					ZipEntry ClassEntry = jarFile.getEntry(ResourceName);

					if (ClassEntry==null)
					{
						continue;
					}
					else
					{
						
						RescourceURL= new URL("jar:file:" + thisPath + "!/" + ResourceName);
					}
					
				}
				else
				{
					//find class byte code from local folder
					File ClassFile=new File(thisPath,ResourceName);
					if (!ClassFile.exists())
					{
						continue;
					}
					else
					{
						
						RescourceURL= new URL("jar:file:" + thisPath + "!/" + ResourceName);
					}
					
				}
				
				System.out.println(" found");
				return RescourceURL;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 		
		}
		
		System.out.println(" not found");
		
		return null;		

	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		System.out.println("Looking for resources:"+name);
		// TODO Auto-generated method stub
		Vector<URL> URLs=new Vector<URL>();
		URL thisURL=getResource(name);
		if (thisURL!=null)
		{
			URLs.add(getResource(name));
		}
		
		Enumeration<URL> E= URLs.elements();
		return E;
	}

	@Override
	public InputStream getResourceAsStream(String ResourceName) {
		
		System.out.print("Looking for resourceasstream:"+ResourceName);
		
		// TODO Auto-generated method stub
		for (String thisPath : ClassPath)
		{
			try {
				InputStream ClassIStream;
				if (thisPath.endsWith(".jar")|| thisPath.endsWith(".zip"))
				{
					//get class byte code from jar file
					JarFile jarFile = new JarFile(thisPath);
					ZipEntry ClassEntry = jarFile.getEntry(ResourceName);

					if (ClassEntry==null)
					{
						continue;
					}
					else
					{
						ClassIStream = jarFile.getInputStream(ClassEntry);					
					}
					
				}
				else
				{
					//find class byte code from local folder
					File ClassFile=new File(thisPath,ResourceName);
					if (!ClassFile.exists())
					{
						continue;
					}
					else
					{
						ClassIStream=new FileInputStream(ClassFile);					
					}
					
				}
				
				System.out.println(" found");
				return ClassIStream;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 		
		}
		
		System.out.println(" not found");
		return null;
	}

	private String[] ClassPath;
	public DynamicClassLoader(String CP)
	{
		String path_separator=System.getProperty("path.separator");
		ClassPath=CP.split(path_separator);
		
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		
		// TODO Auto-generated method stub

		byte[] classBytes = SearchClassByteCode(className);
		if (classBytes != null) {
			return defineClass(className, classBytes, 0, classBytes.length);
		}

		throw new ClassNotFoundException(className);
	}
	
	private byte[] SearchClassByteCode(String className)
	{
		className = className.replace('.', '/');
		className = className.concat(".class");
		
		for (String thisPath : ClassPath)
		{
			
			try {
				InputStream ClassIStream;
				if (thisPath.endsWith(".jar")|| thisPath.endsWith(".zip"))
				{
					//get class byte code from jar file
					JarFile jarFile;
					try {
						jarFile = new JarFile(thisPath);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.err.println("failed to find class: "+className+" in jar:"+ thisPath);
						throw e;
					}
					ZipEntry ClassEntry = jarFile.getEntry(className);

					if (ClassEntry==null)
					{
						continue;
					}
					else
					{
						ClassIStream = jarFile.getInputStream(ClassEntry);					
					}
					
				}
				else
				{
					//find class byte code from local folder
					File ClassFile=new File(thisPath,className);
					if (!ClassFile.exists())
					{
						continue;
					}
					else
					{
						ClassIStream=new FileInputStream(ClassFile);					
					}
					
				}
				
				ByteArrayOutputStream classData = new ByteArrayOutputStream();   
				int ActualReadSize = 0;   
				byte[] buffer=new byte[1024];

				while(ClassIStream.available() != 0) {				        
					ActualReadSize = ClassIStream.read(buffer, 0, 1024);   
					if(ActualReadSize < 0) 
					{
						
						break;   				        	
					}
					classData.write(buffer, 0, ActualReadSize);   
				}   								
				
				byte[] ClassByteCode=classData.toByteArray();
				classData.close();
				ClassIStream.close();
				return ClassByteCode;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 		
		}
		return null;
		
	}
	
	public static void main(String[] args)
	{
		
		
//		String CP="D:/test/Puthurricane/alljars/spring-aop-3.2.10.RELEASE.jar";
//		DynamicClassLoader DCL = new DynamicClassLoader(CP);
//
//		Thread.currentThread().setContextClassLoader(DCL);
//		DCL.getResourceAsStream("META-INF/spring.handlers");

		

		
		String classpath="D:\\test\\Puthurricane\\3.1.6;"+
				"D:\\test\\Puthurricane\\3.1.6\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.1.4\\jackson-databind-2.1.4.jar;"+                                 
				"D:\\test\\Puthurricane\\3.1.6\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.1.4\\jackson-annotations-2.1.4.jar;"+                           
				"D:\\test\\Puthurricane\\3.1.6\\com\\fasterxml\\jackson\\core\\jackson-core\\2.1.4\\jackson-core-2.1.4.jar;"+                                         
				"D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-servlet\\3.1.0\\metrics-servlet-3.1.0.jar;"+                                         
				"D:\\test\\Puthurricane\\3.1.6\\com\\google\\guava\\guava\\14.0.1\\guava-14.0.1.jar;"+                                                                
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\commons\\commons-compress\\1.7\\commons-compress-1.7.jar;"+                                              
				"D:\\test\\Puthurricane\\3.1.6\\org\\tukaani\\xz\\1.4\\xz-1.4.jar;"+                                                                                  
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\esb-transport-oracle\\3.0.2\\esb-transport-oracle-3.0.2.jar;"+                                      
				"D:\\test\\Puthurricane\\3.1.6\\com\\oracle\\ojdbc_g\\12.1.0.2.0\\ojdbc_g-12.1.0.2.0.jar;"+                                                           
				"D:\\test\\Puthurricane\\3.1.6\\com\\oracle\\ucp\\12.1.0.2.0\\ucp-12.1.0.2.0.jar;"+                                                                   
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\esb-app-puthurricanes\\3.1.6\\esb-app-puthurricanes-3.1.6.jar;"+                                    
				"D:\\test\\Puthurricane\\3.1.6\\joda-time\\joda-time\\2.3\\joda-time-2.3.jar;"+                                                                       
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\esb-transport-activemq\\3.3.0\\esb-transport-activemq-3.3.0.jar;"+                                  
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\activemq\\activemq-client\\5.9.1\\activemq-client-5.9.1.jar;"+                                           
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\geronimo\\specs\\geronimo-jms_1.1_spec\\1.1.1\\geronimo-jms_1.1_spec-1.1.1.jar;"+                        
				"D:\\test\\Puthurricane\\3.1.6\\org\\fusesource\\hawtbuf\\hawtbuf\\1.9\\hawtbuf-1.9.jar;"+                                                            
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\geronimo\\specs\\geronimo-j2ee-management_1.1_spec\\1.0.1\\geronimo-j2ee-management_1.1_spec-1.0.1.jar;"+
				"D:\\test\\Puthurricane\\3.1.6\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar;"+                                                              
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\messageio\\3.0.2\\messageio-3.0.2.jar;"+                                                            
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\httpcomponents\\httpclient\\4.2.2\\httpclient-4.2.2.jar;"+                                               
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\httpcomponents\\httpcore\\4.2.2\\httpcore-4.2.2.jar;"+                                                   
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\esb-bootstrap\\4.1.1\\esb-bootstrap-4.1.1.jar;"+                                                    
				"D:\\test\\Puthurricane\\3.1.6\\commons-codec\\commons-codec\\1.6\\commons-codec-1.6.jar;"+                                                           
				"D:\\test\\Puthurricane\\3.1.6\\org\\apache\\httpcomponents\\httpmime\\4.2.2\\httpmime-4.2.2.jar;"+                                                   
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-servlet\\9.2.10.v20150310\\jetty-servlet-9.2.10.v20150310.jar;"+                           
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-security\\9.2.10.v20150310\\jetty-security-9.2.10.v20150310.jar;"+                         
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-context\\3.2.10.RELEASE\\spring-context-3.2.10.RELEASE.jar;"+                            
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-aop\\3.2.10.RELEASE\\spring-aop-3.2.10.RELEASE.jar;"+                                    
				"D:\\test\\Puthurricane\\3.1.6\\aopalliance\\aopalliance\\1.0\\aopalliance-1.0.jar;"+                                                                 
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-beans\\3.2.10.RELEASE\\spring-beans-3.2.10.RELEASE.jar;"+                                
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-core\\3.2.10.RELEASE\\spring-core-3.2.10.RELEASE.jar;"+                                  
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-expression\\3.2.10.RELEASE\\spring-expression-3.2.10.RELEASE.jar;"+                      
				"D:\\test\\Puthurricane\\3.1.6\\com\\pointcarbon\\esb-commons\\3.1.0\\esb-commons-3.1.0.jar;"+                                                        
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-jdbc\\3.2.10.RELEASE\\spring-jdbc-3.2.10.RELEASE.jar;"+                                  
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-tx\\3.2.10.RELEASE\\spring-tx-3.2.10.RELEASE.jar;"+                                      
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-context-support\\3.2.10.RELEASE\\spring-context-support-3.2.10.RELEASE.jar;"+            
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-web\\3.2.10.RELEASE\\spring-web-3.2.10.RELEASE.jar;"+                                    
				"D:\\test\\Puthurricane\\3.1.6\\org\\springframework\\spring-webmvc\\3.2.10.RELEASE\\spring-webmvc-3.2.10.RELEASE.jar;"+                              
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-server\\9.2.10.v20150310\\jetty-server-9.2.10.v20150310.jar;"+                             
				"D:\\test\\Puthurricane\\3.1.6\\javax\\servlet\\javax.servlet-api\\3.1.0\\javax.servlet-api-3.1.0.jar;"+                                              
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-http\\9.2.10.v20150310\\jetty-http-9.2.10.v20150310.jar;"+                                 
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-util\\9.2.10.v20150310\\jetty-util-9.2.10.v20150310.jar;"+                                 
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-io\\9.2.10.v20150310\\jetty-io-9.2.10.v20150310.jar;"+                                     
				"D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-core\\3.1.0\\metrics-core-3.1.0.jar;"+                                               
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-webapp\\9.2.10.v20150310\\jetty-webapp-9.2.10.v20150310.jar;"+                             
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\jetty-xml\\9.2.10.v20150310\\jetty-xml-9.2.10.v20150310.jar;"+                                   
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\websocket\\websocket-server\\9.2.10.v20150310\\websocket-server-9.2.10.v20150310.jar;"+          
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\websocket\\websocket-common\\9.2.10.v20150310\\websocket-common-9.2.10.v20150310.jar;"+          
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\websocket\\websocket-api\\9.2.10.v20150310\\websocket-api-9.2.10.v20150310.jar;"+                
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\websocket\\websocket-client\\9.2.10.v20150310\\websocket-client-9.2.10.v20150310.jar;"+          
				"D:\\test\\Puthurricane\\3.1.6\\org\\eclipse\\jetty\\websocket\\websocket-servlet\\9.2.10.v20150310\\websocket-servlet-9.2.10.v20150310.jar;"+        
				"D:\\test\\Puthurricane\\3.1.6\\org\\slf4j\\slf4j-api\\1.7.6\\slf4j-api-1.7.6.jar;"+                                                                  
				"D:\\test\\Puthurricane\\3.1.6\\org\\slf4j\\log4j-over-slf4j\\1.7.6\\log4j-over-slf4j-1.7.6.jar;"+                                                    
				"D:\\test\\Puthurricane\\3.1.6\\org\\slf4j\\jcl-over-slf4j\\1.7.6\\jcl-over-slf4j-1.7.6.jar;"+                                                        
				"D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-servlets\\3.1.0\\metrics-servlets-3.1.0.jar;"+                                       
				"D:\\test\\Puthurricane\\3.1.6\\org\\slf4j\\jul-to-slf4j\\1.7.6\\jul-to-slf4j-1.7.6.jar;"+                                                            
				"D:\\test\\Puthurricane\\3.1.6\\ch\\qos\\logback\\logback-classic\\1.1.1\\logback-classic-1.1.1.jar;"+                                                
				"D:\\test\\Puthurricane\\3.1.6\\ch\\qos\\logback\\logback-core\\1.1.1\\logback-core-1.1.1.jar;"+                                                      
				"D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-healthchecks\\3.1.0\\metrics-healthchecks-3.1.0.jar;"+                               
				"D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-json\\3.1.0\\metrics-json-3.1.0.jar;"+                                               
		        "D:\\test\\Puthurricane\\3.1.6\\io\\dropwizard\\metrics\\metrics-jvm\\3.1.0\\metrics-jvm-3.1.0.jar";  
		
		DynamicClassLoader DCL = new DynamicClassLoader(classpath);
		try {
			Thread.currentThread().setContextClassLoader(DCL);
			
			
			Class class_main=DCL.loadClass("com.pointcarbon.esb.puthurricanes.Main");
			Method mainMethod = class_main.getMethod("main", String[].class);
			
			mainMethod.invoke(null, (Object)new String[]{"start"});
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		System.out.println("done");
	}
	

}
