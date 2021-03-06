/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connection;

import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.File;
//import java.util.Arrays;
import javax.swing.JOptionPane;


/**
 *
 * @author Sam
 */
public class SSHManager{    
    private JSch jschSSHChannel;
    private String strUserName;
    private String strConnectionIP;
    private String strPassword;
    private static Session sesConnection;
    private String currDir; // TODO: make this a FileInfo
    private File currFile;
    private String tempDir = System.getProperty("java.io.tmpdir");
    private int intTimeOut;

    
 
 /**
   * Construct SSh Channel using given parameters
   * @param  String userName for username
   * @param  String password for password
   * @param  String host for connection IP
   * @param  String knownHostsFileName for filename to set up SSH Channel
   * @exception Throw error if anything wrong with SSH protocol
   * @return No return value.
   */ 
    private void constructorActions(String userName, String password, String host, String knownHostsFileName){
        jschSSHChannel = new JSch();

        try{
            jschSSHChannel.setKnownHosts(knownHostsFileName);
        }
        catch(JSchException jschX){
            System.out.println(jschX.getMessage());
        }

        strUserName = userName;
        strPassword = password;
        strConnectionIP = host;
  }
 
    /**
   * Set up username, password, connectionIP, Host's FileName
   * @param  ConnectionInfo class connection info with username, password, etc.
   */ 
    public SSHManager( ConnectionInfo info ){
        this( info.userName, info.password, info.connectionIP, info.knownHostsFileName );
    }

    /**
   * Construct SSh Channel using given parameters
   * @param  String userName for username
   * @param  String password for password
   * @param  String host for connection IP
   * @param  String knownHostsFileName for filename to set up SSH Channel
   * @return No return value.
   */ 
    public SSHManager(String userName, String password, String host, String knownHostsFileName){
        constructorActions(userName, password, host, knownHostsFileName);
        intTimeOut = 30000;
    }

    
    /**
   * Function to check the error connecting to SSH Channel
   * @return Error Message if any error detected
   */ 
    public String connect(){
        String errorMessage = null;

        try{
            sesConnection = jschSSHChannel.getSession(strUserName, strConnectionIP,22);
            sesConnection.setPassword(strPassword);
            UserInfo ui = new MyUserInfo(){
                /**
                * Function to pop dialogue box out to show message
                * @param String message
                * @return Nothing
                */ 
                public void showMessage(String message){
                    JOptionPane.showMessageDialog(null,message);
                }
                /**
                * Function to check the error connecting to SSH Channel
                * What the hell is this...
                * @return
                */ 
                public boolean promptYesNo(String message){
                    Object[] options={"yes","no"};
                    int foo=JOptionPane.showOptionDialog(null,
                                                         message,
                                                         "warning",
                                                         JOptionPane.DEFAULT_OPTION,
                                                         JOptionPane.WARNING_MESSAGE,
                                                         null,options,options[0]);
                    return foo==0;
                }
            };
            
            sesConnection.setUserInfo(ui);
            sesConnection.connect(intTimeOut);
            
            currDir = sendCommand("pwd");
            currDir = currDir.replace("\n", "").replace("\r", "");
            //FileInfo test = FileInfo(currDir);
            System.out.println();
            
            
        }
        catch(JSchException jschX){
            errorMessage=jschX.getMessage();
        }

        return errorMessage;    //return a string for debugging
    }

    public File getRoot(){
        return new FileInfo(currDir,getLS(currDir));
    }
    
    public static Session getSession(){
        return sesConnection;
    }
    
    public ArrayList< FileInfo > getLS(String dirName){
        String lsString = sendCommand("ls " + dirName + " -l");
        ArrayList<FileInfo> out = new ArrayList<>(); 
        String[] arr = lsString.split("\n");
        
        for(int x=1;x<arr.length;x++){
            //System.out.println(arr[x]);
            FileInfo temp = new FileInfo(arr[x]); // This is a bad approach and I'll fix this eventually
            if( temp.isDirectory() ){
                System.out.println(temp.getLocation());
                
                temp.addElements( getLS(temp.getLocation()) );
            }
            //temp.setPath(currDir);
            out.add(temp);
        }
        return out;
    }
    
    // If called on a file, it will download that file to the temp dir, if
    // it's a directory, it will cd to that command and return the ls -al
    public String downloadFile(FileInfo input){
        String dirTemp = tempDir + "" + input.getName(); // will probably save this to input?
        Channel channel;
        ChannelSftp sftpChannel;
        try{
                channel = sesConnection.openChannel("sftp");
                channel.connect();
                sftpChannel = (ChannelSftp)channel;
        
        if(input.getType().equals("dir")){
            // Do nothing I guess?
        } else{
            sftpChannel.get(input.getLocation(),dirTemp);
        }
            System.out.println("hi");
        System.out.println(sftpChannel.lpwd());
        sftpChannel.disconnect();
        } catch( JSchException | SftpException ioX){
            System.out.println(ioX.getMessage());
            return "";
        }
        
        return dirTemp;
    }
    
    //using a generic command sending method may make things a hell of a lot
    //easier in the futre
    /**
     * This is used to send any command (excluding those involving sftp)
     * to the remote server. 
     * @param command The command to be sent to the remote server
     * @return String which is the exact return from the remote server in
     * response to the sent command.
     */
    public String sendCommand(String command){
        StringBuilder outputBuffer = new StringBuilder();

        try{
            Channel channel = sesConnection.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            channel.connect();
            InputStream commandOutput = channel.getInputStream();
            int readByte = commandOutput.read();

            while(readByte != 0xffffffff){
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }

        channel.disconnect();
        }
     catch(IOException | JSchException ioX){
        System.out.println(ioX.getMessage());
        return null;
     }

     return outputBuffer.toString();
  }
    
    public void disconnect(){
        sesConnection.disconnect();
    }

public static abstract class MyUserInfo
                          implements UserInfo, UIKeyboardInteractive{
    @Override
    public String getPassword(){ return null; }
    @Override
    public boolean promptYesNo(String str){ return false; }
    @Override
    public String getPassphrase(){ return null; }
    @Override
    public boolean promptPassphrase(String message){ return false; }
    @Override
    public boolean promptPassword(String message){ return false; }
    @Override
    public void showMessage(String message){ }
    @Override
    public String[] promptKeyboardInteractive(String destination,
                                              String name,
                                              String instruction,
                                              String[] prompt,
                                              boolean[] echo){
      return null;
    }
    }

  }
