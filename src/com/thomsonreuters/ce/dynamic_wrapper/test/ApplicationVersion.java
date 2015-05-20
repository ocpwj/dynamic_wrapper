package com.thomsonreuters.ce.dynamic_wrapper.test;

import com.thomsonreuters.ce.database.EasyConnection;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

public class ApplicationVersion {

    private static String strSQL = "select id, version, release_media_path, main_class, status from hackathon_application_version where app_id=?";
    private static String UpdateAppVerStatustoreleased = "Update hackathon_application_version set status=1 where app_id=? and status in (2,8)";
    private static String UpdateAppVerStatustostopped = "Update hackathon_application_version set status=7 where app_id=? and status in (4,6)";
    private static String GetRunningAppVersion = "select id from hackathon_application_version where app_id=? and status=5";
    private static String UpdateAppVerStatus = "Update hackathon_application_version set status=? where id=?";
    private static int app_id;

    private static HashMap<Integer, ApplicationVersion> AppVerList = new HashMap<Integer, ApplicationVersion>();

    private int id;

    private String version;
    private String release_media_path;
    private String main_class;
    private ApplicationStatus app_status;
    private DynamicClassLoader DCL = null;

    public ApplicationVersion(int id, String version, String release_media_path, String main_class, ApplicationStatus app_status) {
        super();
        this.id = id;
        this.version = version;
        this.release_media_path = release_media_path;
        this.main_class = main_class;
        this.app_status = app_status;
    }


    public static void LoadAllVersions() {
        Connection DBConn = null;

        try {
            DBConn = new EasyConnection(ApplicationStarter.dbpool_name);
            PreparedStatement objGetAllAppVersions = DBConn.prepareStatement(strSQL);
            objGetAllAppVersions.setInt(1, ApplicationStarter.app_id);
            ResultSet objResult = objGetAllAppVersions.executeQuery();

            while (objResult.next()) {
                int id = objResult.getInt("id");
                String version = objResult.getString("version");
                String release_media_path = objResult.getString("release_media_path");
                String main_class = objResult.getString("main_class");
                ApplicationStatus app_status = ApplicationStatus.getByCode(objResult.getInt("status"));

                AppVerList.put(id, new ApplicationVersion(id, version, release_media_path, main_class, app_status));
            }

            objResult.close();
            objGetAllAppVersions.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                DBConn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void InitializeStatus() throws Exception {
        Connection DBConn = null;

        try {
            DBConn = new EasyConnection(ApplicationStarter.dbpool_name);
            /////////////////////////////////////////////////////////////////////////////
            //update all "installing","uninstalling" version to "released"
            /////////////////////////////////////////////////////////////////////////////
            PreparedStatement objupdatetoreleased = DBConn.prepareStatement(UpdateAppVerStatustoreleased);
            objupdatetoreleased.setInt(1, app_id);
            objupdatetoreleased.executeUpdate();
            objupdatetoreleased.close();


            /////////////////////////////////////////////////////////////////////////////
            //update all "starting","stopping" version to "stopped"
            /////////////////////////////////////////////////////////////////////////////
            PreparedStatement objupdatetostopped = DBConn.prepareStatement(UpdateAppVerStatustostopped);
            objupdatetostopped.setInt(1, app_id);
            objupdatetostopped.executeUpdate();
            objupdatetostopped.close();

            /////////////////////////////////////////////////////////////////////////////
            //Starts running application version
            /////////////////////////////////////////////////////////////////////////////

            PreparedStatement objGetRunningAppVersion = DBConn.prepareStatement(GetRunningAppVersion);
            objGetRunningAppVersion.setInt(1, app_id);
            ResultSet objResult = objGetRunningAppVersion.executeQuery();

            if (objResult.next()) {
                int app_ver_id = objResult.getInt("id");
                ApplicationVersion RunningAv = ApplicationVersion.getByVersion(app_ver_id);
                RunningAv.Start();

            }

            objResult.close();
            objGetRunningAppVersion.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                DBConn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }


    public static ApplicationVersion getByVersion(int ver_id) {
        return AppVerList.get(ver_id);
    }

    public boolean Perform(ControlCmd cmd) {
        try {
            if (cmd.equals(ControlCmd.START) && (app_status.equals(app_status.INSTALLED) || app_status.equals(app_status.STOPPED))) {
                return Start();
            } else if (cmd.equals(ControlCmd.STOP) && (app_status.equals(app_status.STARTED))) {
                return Stop();
            } else if (cmd.equals(ControlCmd.INSTALL) && (app_status.equals(app_status.RELEASED))) {
                return Install();
            } else if (cmd.equals(ControlCmd.UNINSTALL) && (app_status.equals(app_status.STOPPED) || app_status.equals(app_status.INSTALLED))) {
                return Uninstall();
            } else {
                ApplicationStarter.thisLogger.warn("Can not execute command:" + cmd.getName() + " on version:" + this.version);
                return false;
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            return false;
        }

    }

    private boolean Start() throws Exception {
        //update application status to "starting"
        UpdateAppVerStatus(ApplicationStatus.STARTING);

        String ClassPath = ApplicationStarter.install_path + "\\" + version;
        DCL = new DynamicClassLoader(ClassPath);
        Class class_main = DCL.loadClass(main_class);
        Method mainMethod = class_main.getMethod("main", class_main);
        mainMethod.invoke(null, (Object) new String[] {"start"});


        //update status to "started"
        UpdateAppVerStatus(ApplicationStatus.STARTED);

        return true;


    }

    private boolean Stop() throws Exception {
        //update status to "stopping"
        UpdateAppVerStatus(ApplicationStatus.STOPPING);

        if (DCL != null) {
            Class class_main = DCL.loadClass(main_class);
            Method mainMethod = class_main.getMethod("main", class_main);
            mainMethod.invoke(null, (Object) new String[] {"stop"});

        }

        UpdateAppVerStatus(ApplicationStatus.STOPPED);

        return true;

    }

    private boolean Install() throws Exception {
        //update status to "Installing"
        UpdateAppVerStatus(ApplicationStatus.INSTALLING);
        String configURLString = "";//tbd ... http://subversion.commodities.int.thomsonreuters.com/svn/new-web/scripts/deploy/config/esb/ntsretrieval/beta
        String mainJarName = "";//tbd ... ntsretrieval-01.00.08.jar
        File dir = new File(ApplicationStarter.install_path);
        InputStream release = getInstallingStream(release_media_path, null, null);

        GZIPInputStream gis = new GZIPInputStream(release);
        TarInputStream tin = new TarInputStream(gis);
        TarEntry te;
        while ((te = tin.getNextEntry()) != null) {
            if (te.isDirectory()) {
                new File(dir, te.getName()).mkdirs();
                continue;
            }
            String outName = te.getName();
            File out = new File(dir, outName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                if (outName.toLowerCase().endsWith(mainJarName)) {
                    JarInputStream jis = new JarInputStream(tin);
                    try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(fos))) {
                        JarEntry je;
                        while ((je = jis.getNextJarEntry()) != null) {
                            JarEntry outputEntry = new JarEntry(je.getName());
                            outputEntry.setTime(je.getTime());
                            jos.putNextEntry(outputEntry);

                            if (je.getName().toLowerCase().endsWith(".properties")) {
                                InputStream resInputStream = getInstallingStream(configURLString + "/" + je.getName(), "pcadmin", "Hgg41kkt");
                                write(resInputStream, jos);
                            } else {
                                write(jis, jos);
                            }
                        }
                    }
                } else {
                    write(tin, fos);
                }
            }
            //CommandLineUtil.doCommand("chmod -R u+rwx " + new File(dir, "bin").toString());
        }

        UpdateAppVerStatus(ApplicationStatus.INSTALLED);
        return true;


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

    private boolean Uninstall() throws Exception {

        //update status to "stopping"
        UpdateAppVerStatus(ApplicationStatus.UNINSTALLING);


        //update status to "stopped"
        UpdateAppVerStatus(ApplicationStatus.RELEASED);
        return true;
    }


    private void UpdateAppVerStatus(ApplicationStatus AS) {
        Connection DBConn = null;

        try {
            DBConn = new EasyConnection(ApplicationStarter.dbpool_name);
            PreparedStatement objupdateappverstatus = DBConn.prepareStatement(UpdateAppVerStatus);
            objupdateappverstatus.setInt(1, AS.getId());
            objupdateappverstatus.setInt(1, id);
            objupdateappverstatus.executeUpdate();
            objupdateappverstatus.close();
            DBConn.commit();


        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                DBConn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }
}
