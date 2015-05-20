package com.thomsonreuters.ce.dynamic_wrapper.test;

import java.util.zip.ZipEntry;
import java.util.jar.JarFile;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class DynamicClassLoader extends ClassLoader {
	
	@Override
	public InputStream getResourceAsStream(String ResourceName) {
		
		
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
				
				return ClassIStream;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 		
		}
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
		String classpath="/opt/appl/app/nda_cne/taskcontroller/lib/trmessaging.jar";

		
		DynamicClassLoader DCL = new DynamicClassLoader(classpath);
		byte[] test = DCL.SearchClassByteCode("com.thomsonreuters.esp.messaging.TrmlMessage");
		
		System.out.println("done");
	}
	

}
