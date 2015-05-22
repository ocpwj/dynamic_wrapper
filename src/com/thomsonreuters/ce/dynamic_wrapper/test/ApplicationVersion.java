package com.thomsonreuters.ce.dynamic_wrapper.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;




import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.commons.io.FileUtils;

import com.thomsonreuters.ce.database.EasyConnection;

public class ApplicationVersion {
	
	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getRelease_media_path() {
		return release_media_path;
	}


	public void setRelease_media_path(String release_media_path) {
		this.release_media_path = release_media_path;
	}


	public String getMain_class() {
		return main_class;
	}


	public void setMain_class(String main_class) {
		this.main_class = main_class;
	}


	public ApplicationStatus getApp_status() {
		return app_status;
	}


	public void setApp_status(ApplicationStatus app_status) {
		this.app_status = app_status;
	}

	private static String strSQL="select id, version, release_media_path, main_class, status from hackathon_application_version where app_id=?";
	private static String UpdateAppVerStatus="Update hackathon_application_version set status=? where id=?";

	
	private static HashMap<Integer, ApplicationVersion> AppVerList= new HashMap<Integer, ApplicationVersion>();
	
	private int id;
	
	private String version;
	private String release_media_path;
	private String main_class;
	private ApplicationStatus app_status;
	private DynamicClassLoader DCL=null;
	
	public ApplicationVersion(int id, String version,
			String release_media_path, String main_class,
			ApplicationStatus app_status) {
		super();
		this.id = id;
		this.version = version;
		this.release_media_path = release_media_path;
		this.main_class = main_class;
		this.app_status = app_status;
	}
	

	public static void LoadAllVersions()
	{
		Connection DBConn=null;
		
		try {
			DBConn = new EasyConnection(ApplicationStarter.dbpool_name);
			PreparedStatement objGetAllAppVersions=DBConn.prepareStatement(strSQL);
			objGetAllAppVersions.setInt(1, ApplicationStarter.app_id);
			ResultSet objResult=objGetAllAppVersions.executeQuery();

			while (objResult.next())
			{
				int id=objResult.getInt("id");
				String version=objResult.getString("version");
				String release_media_path=objResult.getString("release_media_path");
				String main_class=objResult.getString("main_class");
				ApplicationStatus app_status=ApplicationStatus.getByCode(objResult.getInt("status"));

				AppVerList.put(id, new ApplicationVersion(id,version,release_media_path,main_class,app_status));
			}
			
			objResult.close();
			objGetAllAppVersions.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally
		{
			try {
				DBConn.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		
	}
	

	
	
	
	
	public static ApplicationVersion getByVersion(int ver_id)
	{
		return AppVerList.get(ver_id);
	}
	
	public boolean Perform(ControlCmd cmd, String parameter)
	{
		boolean result;

		if (cmd.equals(ControlCmd.START) && (app_status.equals(app_status.STOPPED)))
		{
			UpdateAppVerStatus(ApplicationStatus.STARTING);	
			result=Start();				
			
			if (result)
			{
				UpdateAppVerStatus(ApplicationStatus.STARTED);				
			}
			else
			{
				UpdateAppVerStatus(ApplicationStatus.STOPPED);
			}
		}
		else if (cmd.equals(ControlCmd.STOP) && (app_status.equals(app_status.STARTED)))
		{
			UpdateAppVerStatus(ApplicationStatus.STOPPING);
			result=Stop();
			if (result)
			{
				UpdateAppVerStatus(ApplicationStatus.STOPPED);				
			}
			else
			{
				UpdateAppVerStatus(ApplicationStatus.STARTED);
			}	
			
		}
		else if (cmd.equals(ControlCmd.INSTALL) && (app_status.equals(app_status.RELEASED)))
		{
			UpdateAppVerStatus(ApplicationStatus.INSTALLING);				
			result=Install(parameter);
			if (result)
			{
				UpdateAppVerStatus(ApplicationStatus.STOPPED);				
			}
			else
			{
				UpdateAppVerStatus(ApplicationStatus.RELEASED);
			}				
		}
		else if (cmd.equals(ControlCmd.UNINSTALL) && (app_status.equals(app_status.STOPPED) ))
		{

			UpdateAppVerStatus(ApplicationStatus.UNINSTALLING);
			result=Uninstall();
			if (result)
			{
				UpdateAppVerStatus(ApplicationStatus.RELEASED);				
			}
			else
			{
				UpdateAppVerStatus(ApplicationStatus.STOPPED);
			}		
			
		}
		else
		{
			ApplicationStarter.thisLogger.warn("Can not execute command:"+cmd.getName()+" on version:" + this.version);
			result=false;
		}
		
		return result;

		

	}
	
	public boolean Start()
	{
		try {
			ApplicationStarter.thisLogger.info("Starting application:"+ApplicationStarter.app_name+" version:"+version);

			String version_install_path=ApplicationStarter.install_path+"/"+version;
			DCL=new DynamicClassLoader(GetClassPath(version_install_path));
			Thread.currentThread().setContextClassLoader(DCL);
			Class class_main=DCL.loadClass(main_class);
			Method mainMethod = class_main.getMethod("main", String[].class);

			try {
				mainMethod.invoke(null, (Object)new String[]{"start"});
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				ApplicationStarter.thisLogger.error(e.getTargetException());
			}

			ApplicationStarter.thisLogger.info("Application:"+ApplicationStarter.app_name+" version:"+version+" has been started");

			return true;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			ApplicationStarter.thisLogger.error("Error occured while executing start command.", e);
			return false;
		}		

	}
	
	private boolean Stop() 
	{
		
		try {
			ApplicationStarter.thisLogger.info("Stopping application:"+ApplicationStarter.app_name+" version:"+version);

			if (DCL!=null)
			{
				Class class_main=DCL.loadClass(main_class);
				Method mainMethod = class_main.getMethod("main", String[].class);			
				
				try {
					mainMethod.invoke(null, (Object)new String[]{"stop"});
				} catch (InvocationTargetException e) {
					ApplicationStarter.thisLogger.error(e.getTargetException());
				}
				
				DCL=null;
				System.gc();
			}

			ApplicationStarter.thisLogger.info("Application:"+ApplicationStarter.app_name+" version:"+version+" has been stopped");
			return true;
			
		} catch (Exception e) {
			ApplicationStarter.thisLogger.error("Error occured while executing stop command.", e);
			return false;
		}

	}
	
	private boolean Install(String configURLString) 
	{

		try {
			ApplicationStarter.thisLogger.info("Installing application:"+ApplicationStarter.app_name+" version:"+version+" to folder:"+ApplicationStarter.install_path+"/"+version);
			String mainJarName = ApplicationStarter.app_name+"-"+version+".jar";
			File dir = new File(ApplicationStarter.install_path+"/"+version);
			if (dir.exists())
			{
				FileUtils.cleanDirectory(dir);
			}

			InputStream release = getInstallingStream(release_media_path, null, null);

			GZIPInputStream gis = new GZIPInputStream(release);
			TarInputStream tin = new TarInputStream(gis);
			TarEntry te;
			while ((te = tin.getNextEntry()) != null) {
				if (te.isDirectory()) {            	
					new File(dir, te.getName()).mkdirs();
					ApplicationStarter.thisLogger.info("Created folder:"+te.getName());
					continue;
				}
				String outName = te.getName();
				File out = new File(dir, outName);
				try (FileOutputStream fos = new FileOutputStream(out)) {
					if (outName.toLowerCase().endsWith(mainJarName)) {
						ApplicationStarter.thisLogger.info("Found main application jar file:"+outName);

						JarInputStream jis = new JarInputStream(tin);
						try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(fos))) {
							JarEntry je;
							while ((je = jis.getNextJarEntry()) != null) {
								JarEntry outputEntry = new JarEntry(je.getName());
								outputEntry.setTime(je.getTime());
								jos.putNextEntry(outputEntry);

								if (je.getName().toLowerCase().endsWith(".properties")) {
									try(InputStream resInputStream = getInstallingStream(configURLString + "/" + je.getName(), "pcadmin", "Hgg41kkt")){
										write(resInputStream, jos);
									}
									catch (Exception e){
										//System.out.println(e.getMessage());
										write(jis, jos);
									}
								} else {
									write(jis, jos);
								}
							}
						}
					} else {
						write(tin, fos);
						ApplicationStarter.thisLogger.info("Installed "+out.getAbsolutePath());
					}
				}
			}		

			ApplicationStarter.thisLogger.info("Application:"+ApplicationStarter.app_name+" version:"+version+" has been installed to folder:"+ApplicationStarter.install_path+"/"+version);
			return true;

		} catch (Exception e) {
			ApplicationStarter.thisLogger.error("Error occured while executing install command with parameter \""+configURLString+"\" .", e);
			return false;
		}		

	}
	
    private void write(InputStream is, OutputStream os) throws Exception {
        byte[] b = new byte[8192];
        int c;
        while ((c = is.read(b, 0, b.length)) != -1) {
            os.write(b, 0, c);
        }
    }

    private InputStream getInstallingStream(String urlString, String username, String password) throws Exception {
        URL u = new URL(urlString);
        InputStream is;
        if (username != null) {
            String userPassword = username + ":" + password;
            String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
            URLConnection uc = u.openConnection();
            uc.setRequestProperty("Authorization", "Basic " + encoding);
            is = uc.getInputStream();
        } else {
            is = u.openStream();
        }
        return is;
    }


	private boolean Uninstall()
	{
		try{
			ApplicationStarter.thisLogger.info("Uninstalling application:"+ApplicationStarter.app_name+" version:"+version+" in folder:"+ApplicationStarter.install_path+"/"+version);
			FileUtils.deleteDirectory(new File(ApplicationStarter.install_path,version));
			ApplicationStarter.thisLogger.info("Application:"+ApplicationStarter.app_name+" version:"+version+" in folder:"+ApplicationStarter.install_path+"/"+version +" has been uninstalled");
			return true;
		} catch (Exception e) {
			ApplicationStarter.thisLogger.error("Error occured while executing uninstall command.", e);
			return false;
		}	
	}
	
	
	private void UpdateAppVerStatus(ApplicationStatus AS)
	{
		
		Connection DBConn=null;
		
		try {
			DBConn = new EasyConnection(ApplicationStarter.dbpool_name);
			PreparedStatement objupdateappverstatus=DBConn.prepareStatement(UpdateAppVerStatus);
			objupdateappverstatus.setInt(1, AS.getId());
			objupdateappverstatus.setInt(2, id);
			objupdateappverstatus.executeUpdate();
			objupdateappverstatus.close();
			DBConn.commit();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally
		{
			try {
				DBConn.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		
		this.app_status=AS;
		
	}
	
	private static String GetClassPath(String install_path)
	{
		StringBuffer sbPath=new StringBuffer();
		Properties prop = new Properties();
		try {
			FileInputStream fis = new FileInputStream(install_path+"/etc/wrapper.conf");
			prop.load(fis);
		} catch (Exception e) {
			throw new RuntimeException("Error in parsing warpper.conf");
		}
		
		TreeMap<Integer, String> pathMap=new TreeMap<Integer, String>();
		
		Iterator it= prop.entrySet().iterator();
		while(it.hasNext()){
		    Map.Entry<String, String> entry=(Map.Entry<String, String>)it.next();
		    String key = entry.getKey();
		    if (key.startsWith("wrapper.java.classpath."))
		    {
		    	int pathorder=Integer.valueOf(key.substring(23));
		    	
		    	String classpath=entry.getValue();
		    	if (classpath.startsWith("%REPO_DIR%"))
		    	{
		    		classpath=install_path+"/repo/"+classpath.substring(11);
		    	}
		    	else
		    	{
		    		classpath=install_path+"/"+classpath;
		    	}
		    			    			
		    	pathMap.put(pathorder, classpath);
		    }
		}
		
		String path_separator=System.getProperty("path.separator");
				
		it = pathMap.entrySet().iterator();         
		while(it.hasNext()){      
		     Map.Entry<String, String> entry1=(Map.Entry<String, String>)it.next();    
		     
		     if (sbPath.length()==0)
		     {
		    	 sbPath.append(entry1.getValue());  
		     }
		     else
		     {
		    	 sbPath.append(path_separator).append(entry1.getValue());  
		     }		     
		}   
		
		return sbPath.toString();
        

	}
	
    public static void writeToArr(File dir, FilenameFilter searchSuffix, StringBuffer Classpath) {
        
        File []files = dir.listFiles();
        for(File f : files){
            if(f.isDirectory()){
                //ตÝน้มหกฃ
                writeToArr(f, searchSuffix, Classpath);       
            }else{
                if(searchSuffix.accept(dir, f.getName())){
                    Classpath.append(";"+f.getAbsolutePath());
                }
            }
        }
    }
    
}
