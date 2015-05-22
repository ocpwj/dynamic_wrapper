package com.thomsonreuters.ce.dynamic_wrapper.test;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.HashMap;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.dcn.DatabaseChangeEvent;
import oracle.jdbc.dcn.DatabaseChangeListener;
import oracle.jdbc.dcn.DatabaseChangeRegistration;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.thomsonreuters.ce.database.EasyConnection;
import com.thomsonreuters.ce.dbor.server.SrvControl;

public class ApplicationStarter implements SrvControl {
	
	private static ApplicationStarter ApplicationStarter=null;
	private static String app_config="../cfg/application.cfg";
	private static String log_config="../cfg/logging.cfg";
	private static String dbpool_cfg="../cfg/dbpool.cfg";
	
	protected static String dbpool_name="CEF_CNR";
	protected static String app_name;
	protected static int app_id=0;
	protected static String install_path;
	protected static Logger thisLogger=null;
	private ControlCmd currentCMD=null;

	
	private String GetAppIDbyName="select id from hackathon_application where app_name=?"; 
	
	private String GetNewCommandDetails="select hav.id as ver_id,hac.app_ctrl_cmd_id as cmd_id, parameter as parameter from hackathon_application_control hac,hackathon_application_version hav where hac.rowid=? and hac.app_ctrl_cmd_status=1 and hac.app_version_id=hav.id and hav.app_id = ?";
	private String FailOutstandingCommands="update hackathon_application_control set app_ctrl_cmd_status=4  where app_ctrl_cmd_status in (1,2) and app_version_id in (select id from hackathon_application_version where app_id=?)";
	private static String UpdateAppVerStatustoreleased="Update hackathon_application_version set status=1 where app_id=? and status in (2,8)";
	private static String UpdateAppVerStatustostopped="Update hackathon_application_version set status=7 where app_id=? and status in (4,5,6)";
	private static String GetRunningAppVersion="select id from hackathon_application_version where app_id=? and status=5";
	
	private String UpdateCtrlCommandStatus="update hackathon_application_control set app_ctrl_cmd_status=?  where rowid=?";
	
	

	@Override
	public void Start(Properties arg0) {
		// TODO Auto-generated method stub
		
		System.setSecurityManager(new NoExitSecurityManager());
		
		EasyConnection DBConn=null;
		
		try {
			/////////////////////////////////////////////////////////////////////////////
			// Initialize logging
			/////////////////////////////////////////////////////////////////////////////
			PropertyConfigurator.configure(log_config);
			thisLogger=Logger.getLogger("dynamicwrapper");
			thisLogger.info("Logging is working");		
			
			/////////////////////////////////////////////////////////////////////////////
			//Initialize database connection pool
			/////////////////////////////////////////////////////////////////////////////
			EasyConnection.configPool(dbpool_cfg);
			thisLogger.info("Database connection pool is working");
			
			app_name=arg0.getProperty("application");
			install_path=arg0.getProperty("install_path");
				
			/////////////////////////////////////////////////////////////////////////////
			//Get App_id by name
			/////////////////////////////////////////////////////////////////////////////
			DBConn = new EasyConnection(dbpool_name);
			
			PreparedStatement objGetAppIDbyName=DBConn.prepareStatement(GetAppIDbyName);
			objGetAppIDbyName.setString(1, app_name);
			ResultSet objResult=objGetAppIDbyName.executeQuery();
		
			if (!objResult.next())
			{
				thisLogger.error("Invalid application name:"+app_name);
				System.exit(1);
			}
			
			app_id=objResult.getInt("id");
			objResult.close();
			objGetAppIDbyName.close();
			
			/////////////////////////////////////////////////////////////////////////////
			//Initialize existing application version status
			/////////////////////////////////////////////////////////////////////////////
			InitializeStatus();
			
			/////////////////////////////////////////////////////////////////////////////
			//Fail outstanding tasks in queue
			/////////////////////////////////////////////////////////////////////////////		
			PreparedStatement objGetOutstandingCmd=DBConn.prepareStatement(FailOutstandingCommands);
			objGetOutstandingCmd.setInt(1, app_id);
			objGetOutstandingCmd.executeUpdate();
			DBConn.commit();			
			objResult.close();
			objGetOutstandingCmd.close();     
			
			/////////////////////////////////////////////////////////////////////////////
			//Register DB change listener
			/////////////////////////////////////////////////////////////////////////////
	        Properties prop = new Properties();
	        prop.setProperty(OracleConnection.DCN_NOTIFY_ROWIDS, "true");
	        prop.setProperty(OracleConnection.DCN_IGNORE_DELETEOP, "true");
	        prop.setProperty(OracleConnection.DCN_IGNORE_UPDATEOP, "true");
	        OracleConnection RAWConn=DBConn.getRawConnection();
	        DatabaseChangeRegistration databaseChangeRegistration = RAWConn.registerDatabaseChangeNotification(prop);
	        
	        databaseChangeRegistration.addListener(new DatabaseChangeListener() { 
                public void onDatabaseChangeNotification(DatabaseChangeEvent databaseChangeEvent) {
        			ApplicationStarter.CommandHandler(databaseChangeEvent);
                }
            });
	        
	        Statement stmt = DBConn.createStatement();
            ((OracleStatement) stmt).setDatabaseChangeRegistration(databaseChangeRegistration);
            ResultSet rs = stmt.executeQuery("SELECT * FROM hackathon_application_control");
            while (rs.next()) {
            }
            rs.close();
            stmt.close();

			
            thisLogger.info("Listening new command to table hackathon_application_control");
			
			            
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally
		{
//			try {
//				DBConn.close();
//			} catch (SQLException ex) {
//				ex.printStackTrace();
//			}
		}
		
	}
	
	protected void CommandHandler(DatabaseChangeEvent e)
	{
		String rowid=e.getTableChangeDescription()[0].getRowChangeDescription()[0].getRowid().stringValue();
		thisLogger.info("Received a new command on rowid:"+rowid);
		
		Connection DBConn = null;
		
		try {
			DBConn = new EasyConnection(dbpool_name);
			
			PreparedStatement objGetNewCommandDetails=DBConn.prepareStatement(GetNewCommandDetails);
			objGetNewCommandDetails.setString(1, rowid);
			objGetNewCommandDetails.setInt(2, app_id);
			ResultSet objResult=objGetNewCommandDetails.executeQuery();

			if (!objResult.next())
			{
				objResult.close();
				objGetNewCommandDetails.close();
				thisLogger.info("new command on rowid:"+rowid+" has nothing to do with app:"+app_name);
				return;
			}
						
			//Update control command status to running
			UpdateCtrlCmdStatus(rowid,CmdStatus.RUNNING);
			
			int app_ver_id=objResult.getInt("ver_id");
			ControlCmd cmd=ControlCmd.getByCode(objResult.getInt("cmd_id"));
			
			ApplicationVersion av=ApplicationVersion.getByVersion(app_ver_id);
			
			synchronized(this)
			{
				if (currentCMD!=null)
				{
					thisLogger.warn("Can not respond to new command: "+cmd.getName()+", as there's an ongoing command: "+currentCMD.getName()+" running on version: "+ av.getVersion());
					UpdateCtrlCmdStatus(rowid,CmdStatus.FAILED);
					objResult.close();
					objGetNewCommandDetails.close();
					return;
				}
				else
				{
					currentCMD=cmd;
				}
			}
			
			
			
			String parameter=objResult.getString("parameter");
			
			
			

			boolean result =av.Perform(cmd,parameter);
			
			if (result)
			{
				UpdateCtrlCmdStatus(rowid,CmdStatus.FINISHED);
			}
			else
			{
				UpdateCtrlCmdStatus(rowid,CmdStatus.FAILED);
			}
						
			objResult.close();
			objGetNewCommandDetails.close();
			
			synchronized(this)
			{
				currentCMD=null;
			}			
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally
		{
			try {
				DBConn.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			
		}
	}
	
	
	public void UpdateCtrlCmdStatus(String rowid, CmdStatus cs )
	{
		Connection DBConn=null;
		
		try {
			DBConn = new EasyConnection(dbpool_name);
			PreparedStatement objupdatectrlcmdstatus=DBConn.prepareStatement(UpdateCtrlCommandStatus);
			objupdatectrlcmdstatus.setInt(1, cs.getId());
			objupdatectrlcmdstatus.setString(2, rowid);
			objupdatectrlcmdstatus.executeUpdate();
			objupdatectrlcmdstatus.close();
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
		
	}
	
	public static void InitializeStatus() throws Exception
	{
		Connection DBConn=null;
		
		try {
			DBConn = new EasyConnection(dbpool_name);
			
			/////////////////////////////////////////////////////////////////////////////
			//Get running application version
			/////////////////////////////////////////////////////////////////////////////				
			PreparedStatement objGetRunningAppVersion=DBConn.prepareStatement(GetRunningAppVersion);
			objGetRunningAppVersion.setInt(1, app_id);
			ResultSet objResult=objGetRunningAppVersion.executeQuery();

			int running_app_ver_id=0;
			
			if (objResult.next())
			{
				running_app_ver_id=objResult.getInt("id");

			}
			
			thisLogger.info("Updating previous version status");
			
			/////////////////////////////////////////////////////////////////////////////
			//update all "installing","uninstalling" version to "released"
			/////////////////////////////////////////////////////////////////////////////
			PreparedStatement objupdatetoreleased=DBConn.prepareStatement(UpdateAppVerStatustoreleased);
			objupdatetoreleased.setInt(1, app_id);
			objupdatetoreleased.executeUpdate();
			objupdatetoreleased.close();

			/////////////////////////////////////////////////////////////////////////////
			//update all "starting","started","stopping" version to "stopped"
			/////////////////////////////////////////////////////////////////////////////
			PreparedStatement objupdatetostopped=DBConn.prepareStatement(UpdateAppVerStatustostopped);
			objupdatetostopped.setInt(1, app_id);
			objupdatetostopped.executeUpdate();			
			objupdatetostopped.close();
			
			DBConn.commit();
			
			ApplicationVersion.LoadAllVersions();
			
			/////////////////////////////////////////////////////////////////////////////
			//Starts running application version
			/////////////////////////////////////////////////////////////////////////////	
			if (running_app_ver_id!=0)
			{
				thisLogger.info("Starting application which was running previously");
				ApplicationVersion RunningAv=ApplicationVersion.getByVersion(running_app_ver_id);
				// RunningAv=ApplicationVersion.getByVersion(running_app_ver_id);
				RunningAv.Perform(ControlCmd.START,"");
			}			
			
			objResult.close();
			objGetRunningAppVersion.close();
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

	@Override
	public void Stop() {
		// TODO Auto-generated method stub
		System.setSecurityManager(new SecurityManager());

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length > 0 && "stop".equals(args[0])) {

			ApplicationStarter.Stop();
			System.exit(0);
		}

		/////////////////////////////////////////////////////////////////////////////
		// Read config file into prop object
		/////////////////////////////////////////////////////////////////////////////
		Properties prop = new Properties();
		try {
			FileInputStream fis = new FileInputStream(app_config);
			prop.load(fis);
		} catch (Exception e) {
			System.out.println("Can't read application configuration file: " + app_config);
		}		 

		//Start service
		ApplicationStarter=new ApplicationStarter();
		ApplicationStarter.Start(prop);

	}

}
