/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.auxilary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mvg.exceptions.LoginException;
import mvg.managers.SessionManager;
import mvg.model.MVGObject;
import mvg.model.Error;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;

/**
 *
 * @author ghost
 */
public class RemoteComms
{
    public static String master_ip = "localhost";
    public static int master_port = 8083;
    public static String host = "http://"+master_ip+":"+master_port;
    public static String webserver_url = "http://95.85.57.110:9999";
    public static final String TAG = "RemoteComms";

    public static Session auth_legacy(String usr, String pwd) throws IOException, LoginException
    {
        ArrayList<AbstractMap.SimpleEntry<String,String>> data = new ArrayList<>();
        data.add(new AbstractMap.SimpleEntry<>("username",usr));
        data.add(new AbstractMap.SimpleEntry<>("password",pwd));

        System.out.println("usr: "  + usr + ", pwd: " + pwd);

        HttpURLConnection connObj = postData("/api/auth", data, null);
        
        if(connObj.getResponseCode()==200)
        {
            String cookie = connObj.getHeaderField("Set-Cookie");
            if(cookie!=null)
            {
                String[] cookie_attrs = cookie.split(";");
                if(cookie_attrs.length>=3)
                {
                    String session_id="";//cookie_attrs[0];
                    int date=0, ttl=0;
                    for(String attr: cookie_attrs)
                    {
                        if(attr.contains("="))
                        {
                            String key = attr.split("=")[0];
                            String val = attr.split("=")[1];
                            switch(key.toUpperCase())
                            {
                                case "SESSION":
                                    session_id = val;
                                    break;
                                case "DATE":
                                    date = (int)Double.parseDouble(val);
                                    break;
                                case "TTL":
                                    ttl = Integer.parseInt(val);
                                    break;
                                default:
                                    System.err.println("Unknown cookie attribute: " + key);
                                    break;
                            }
                        }else{
                            throw new LoginException("Cookie attributes are invalid. Missing '='.");
                        }
                    }
                    
                    Session session = new Session(usr, session_id, date, ttl);
                    connObj.disconnect();
                    return session;
                }else{
                    connObj.disconnect();
                    throw new LoginException("Cookie attributes are invalid. Not enough attributes. Must be >= 3.");
                }
            }else{
                connObj.disconnect();
               throw new LoginException("Cookie object is not set.");
            }
        }else{
            connObj.disconnect();
            if(connObj.getResponseCode()==404)
                throw new LoginException("Invalid credentials.");
            else
                throw new LoginException("Could not authenticate, server response code: " + connObj.getResponseCode());
        }
    }

    public static Session auth(String usr, String pwd) throws IOException, LoginException
    {
        ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
        headers.add(new AbstractMap.SimpleEntry<>("usr", usr));
        headers.add(new AbstractMap.SimpleEntry<>("pwd", pwd));
        HttpURLConnection connObj = putJSONData("/auth", null, headers);

        if(connObj.getResponseCode()==200)
        {
            //String cookie = connObj.getHeaderField("Set-Cookie");
            String session_str = IO.readStream(connObj.getInputStream());
            Session session = new GsonBuilder().create().fromJson(session_str, Session.class);
            if(session!=null)
            {
                IO.log("User Authenticator", IO.TAG_INFO, "successfully signed in.");
                connObj.disconnect();
                return session;
            }else{
                connObj.disconnect();
                IO.logAndAlert("Authentication Error", "Could not parse Session JSON object.", IO.TAG_ERROR);
                //throw new LoginException("Cookie object is not set.");
            }
        }else{
            connObj.disconnect();
            IO.log("User Authenticator", IO.TAG_ERROR, "could not sign in.");
            if(connObj.getResponseCode()==HttpURLConnection.HTTP_NOT_FOUND)
                IO.logAndAlert("Authentication Error", "Invalid credentials.\nPlease try again with valid credentials or reset your password if you have forgotten it.", IO.TAG_ERROR);
            /*if(connObj.getResponseCode()==404)
                throw new LoginException("Invalid credentials.");
            else
                throw new LoginException("Could not authenticate, server response code: " + connObj.getResponseCode());*/
        }
        return null;
    }

    public static void setHost(String h)
    {
        host = h;
    }

    public static boolean pingServer() throws IOException
    {
        URL urlConn = new URL(host);
        HttpURLConnection httpConn =  (HttpURLConnection)urlConn.openConnection();

        boolean response = (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK);
        httpConn.disconnect();
        return response;
    }

    public static String sendGetRequest(String url, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        IO.log(TAG, IO.TAG_INFO, String.format("\nGET %s HTTP/1.1\nHost: %s", url, host));

        URL urlConn = new URL(host + url);
        HttpURLConnection httpConn =  (HttpURLConnection)urlConn.openConnection();
        for(AbstractMap.SimpleEntry<String,String> header:headers)
            httpConn.setRequestProperty(header.getKey() , header.getValue());
        
        String response = null;
        if(httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            response="";
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line="";
            int read=0;
            while ((line=in.readLine())!=null)
                response += line;
            //Log.d(TAG,response);
        }else
        {
            response="";
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
            String line="";
            int read=0;
            while ((line=in.readLine())!=null)
                response += line;
        }

        IO.log(TAG, IO.TAG_INFO, "GET response> " + response + "\n");
        return response;
    }

    public static String sendGetRequestByURL(String url, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        IO.log(TAG, IO.TAG_INFO, String.format("\nGET %s HTTP/1.1\nHost: %s", url, host));

        URL urlConn = new URL(url);
        HttpURLConnection httpConn =  (HttpURLConnection)urlConn.openConnection();
        for(AbstractMap.SimpleEntry<String,String> header:headers)
            httpConn.setRequestProperty(header.getKey() , header.getValue());

        String response = null;
        if(httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            response="";
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line="";
            int read=0;
            while ((line=in.readLine())!=null)
                response += line;
            //Log.d(TAG,response);
        }else
        {
            response="";
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
            String line="";
            int read=0;
            while ((line=in.readLine())!=null)
                response += line;
        }

        IO.log(TAG, IO.TAG_INFO, "GET response> " + response + "\n");
        return response;
    }

    public static byte[] sendFileRequest(String filename, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        IO.log(TAG, IO.TAG_INFO, String.format("\nGET %s HTTP/1.1", filename));

        URL urlConn = new URL(host + filename);
        //URL urlConn = new URL("http://127.0.0.1:9000/api/file/inspection/3-demolition.pdf");
        try(InputStream in = urlConn.openStream())
        {
            //Files.copy(in, new File("download.pdf").toPath(), StandardCopyOption.REPLACE_EXISTING);
            //DataInputStream dataInputStream = new DataInputStream(in);

            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read=0;
            while ((read=in.read(buffer, 0, buffer.length))>0)
                outbytes.write(buffer, 0, read);
            outbytes.flush();
            in.close();
            IO.log(TAG, IO.TAG_INFO, "GET received file> " + filename + " " + outbytes.toByteArray().length + " bytes.\n");
            return outbytes.toByteArray();
        }
        //URL urlConn = new URL(host);
        /*HttpURLConnection httpConn =  (HttpURLConnection)urlConn.openConnection();

        for(AbstractMap.SimpleEntry<String,String> header:headers)
            httpConn.setRequestProperty(header.getKey() , header.getValue());


        String response = null;
        if(httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            response="";
            DataInputStream in = new DataInputStream(httpConn.getInputStream());

            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read=0;
            while ((read=in.read(buffer, 0, buffer.length))>0)
            {
                outbytes.write(buffer, 0, read);
            }
            outbytes.flush();
            in.close();
            IO.log(TAG, IO.TAG_INFO, "GET received file> " + filename + " " + outbytes.toByteArray().length + "bytes.\n");
            return outbytes.toByteArray();
        }else
        {
            IO.log(TAG, IO.TAG_ERROR, IO.readStream(httpConn.getErrorStream()));
            return null;
        }*/
    }

    public static byte[] sendFileRequestByURL(String url, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        IO.log(TAG, IO.TAG_INFO, String.format("\nGET %s HTTP/1.1", url));

        URL urlConn = new URL(url);
        try(InputStream in = urlConn.openStream())
        {
            //Files.copy(in, new File("download.pdf").toPath(), StandardCopyOption.REPLACE_EXISTING);
            //DataInputStream dataInputStream = new DataInputStream(in);

            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read=0;
            while ((read=in.read(buffer, 0, buffer.length))>0)
                outbytes.write(buffer, 0, read);
            outbytes.flush();
            in.close();
            IO.log(TAG, IO.TAG_INFO, "GET received file> " + url + " " + outbytes.toByteArray().length + " bytes.\n");
            return outbytes.toByteArray();
        }
    }

    public static HttpURLConnection postData(String function, ArrayList<AbstractMap.SimpleEntry<String,String>> params, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
                httpConn.setRequestProperty(header.getKey() , header.getValue());
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("POST");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        //Encode body data in UTF-8 charset
        StringBuilder result = new StringBuilder();
        for(int i=0;i<params.size();i++)
        {
            AbstractMap.SimpleEntry<String,String> entry = params.get(i);
            System.out.println(entry);
            if(entry!=null)
            {
                if(entry.getKey()!=null && entry.getValue()!=null)
                {
                    result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                    result.append((i != params.size() - 1 ? "&" : ""));
                }else return null;
            }else return null;
        }

        IO.log(TAG, IO.TAG_INFO, String.format("POST %s HTTP/1.1\nHost: %s", function, host));

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(result.toString());
        writer.flush();
        writer.close();
        os.close();

        //httpConn.connect();
        
        /*Scanner scn = new Scanner(new InputStreamReader(httpConn.getErrorStream()));
        String resp = "";
        while(scn.hasNext())
            resp+=scn.nextLine();
        System.err.println(resp);*
        String resp = httpConn.getHeaderField("Set-Cookie");
        System.err.println(resp);*/
        
        return httpConn;
    }

    public static HttpURLConnection postData(String function, String object, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
                httpConn.setRequestProperty(header.getKey() , header.getValue());
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("POST");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        IO.log(TAG, IO.TAG_INFO, String.format("POST %s HTTP/1.1\nHost: %s", function, host));

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(object);
        writer.flush();
        writer.close();
        os.close();

        return httpConn;
    }

    public static void updateObjectOnServer(MVGObject object, String api_method, String property)
    {
        if(SessionManager.getInstance().getActive()!=null)
        {
            if(!SessionManager.getInstance().getActive().isExpired())
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                String id = object.get_id();

                if(id!=null)
                {
                    headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                    headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                    try
                    {
                        HttpURLConnection connection = RemoteComms.postJSON(api_method, object.toString(), headers);
                        if(connection!=null)
                        {
                            if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                                IO.log(TAG, IO.TAG_INFO, "Successfully updated MVGObject's '" + property + "' property.");
                            else
                            {
                                String msg = IO.readStream(connection.getErrorStream());
                                Gson gson = new GsonBuilder().create();
                                Error error = gson.fromJson(msg, Error.class);
                                IO.logAndAlert("Error " +String.valueOf(connection.getResponseCode()), error.getError(), IO.TAG_ERROR);
                            }
                            connection.disconnect();
                        }else IO.logAndAlert("Error", "Connection to server was interrupted.", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.logAndAlert(TAG, e.getMessage(), IO.TAG_ERROR);
                    }
                } else IO.log(TAG, IO.TAG_ERROR, "Invalid MVGObject ID");
            }else{
                IO.logAndAlert("Session expired", "No active sessions.", IO.TAG_ERROR);
            }
        }else{
            IO.logAndAlert("Error", "Connection to server was interrupted.", IO.TAG_ERROR);
            JOptionPane.showMessageDialog(null, "No active sessions.", "Session expired", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void uploadFile(String endpoint, ArrayList<AbstractMap.SimpleEntry<String,String>> headers, byte[] file) throws IOException
    {
        URL urlConn = new URL(host + endpoint);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
                httpConn.setRequestProperty(header.getKey() , header.getValue());

        httpConn.setRequestProperty("Content-Length", String.valueOf(file.length));
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("POST");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        IO.log(TAG, IO.TAG_INFO, String.format("POST %s HTTP/1.1\nHost: %s", endpoint, host));

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        //OutputStreamWriter writer = new OutputStreamWriter(os);
        os.write(file);
        os.flush();
        os.close();

        httpConn.connect();
        String desc = IO.readStream(httpConn.getInputStream());
        IO.logAndAlert("File Upload", httpConn.getResponseCode() + ":\t" + desc, IO.TAG_INFO);
        httpConn.disconnect();

    }

    public static HttpURLConnection putJSONData(String function, ArrayList<AbstractMap.SimpleEntry<String,String>> params, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        /*URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
            {
                IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "setting header: ["+header.getKey()+":"+header.getValue()+"]");
                httpConn.setRequestProperty(header.getKey(), header.getValue());
            }
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("PUT");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);*/

        //Encode body data in UTF-8 charset
        StringBuilder result = new StringBuilder("{");
        if(params!=null)
        {
            for (int i = 0; i < params.size(); i++)
            {
                AbstractMap.SimpleEntry<String, String> entry = params.get(i);
                if (entry != null)
                {
                    if (entry.getKey() != null && entry.getValue() != null)
                    {
                        //if not first item, add commas
                        if(i>0)
                            result.append(",");
                        result.append("\"" + entry.getKey() + "\"");
                        result.append(':');
                        result.append("\"" + entry.getValue() + "\"");
                        //result.append((i != params.size() - 1 ? "," : ""));//add comma if not last param
                    } else
                    {
                        IO.log(RemoteComms.class.getName(), IO.TAG_ERROR, "invalid key-value pair for entry: [" + entry.getKey()+","+entry.getValue()+"]");
                        return null;
                    }
                } else
                {
                    IO.log(RemoteComms.class.getName(), IO.TAG_ERROR, "invalid entry");
                    return null;
                }
            }
        }
        result.append("}");
        /*IO.log(TAG, IO.TAG_INFO, String.format("%s %s HTTP/1.1\nHost: %s", httpConn.getRequestMethod(), function, host));

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(result.toString());
        writer.flush();
        writer.close();
        os.close();

        //httpConn.connect();

        /*Scanner scn = new Scanner(new InputStreamReader(httpConn.getErrorStream()));
        String resp = "";
        while(scn.hasNext())
            resp+=scn.nextLine();
        System.err.println(resp);*
        String resp = httpConn.getHeaderField("Set-Cookie");
        System.err.println(resp);*

        return httpConn;*/
        return putJSON(function, result.toString(), headers);
    }

    public static HttpURLConnection patchJSON(String function, String object, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
            {
                IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "setting header: ["+header.getKey()+":"+header.getValue()+"]");
                httpConn.setRequestProperty(header.getKey(), header.getValue());
            }
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        httpConn.setRequestMethod("POST");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        IO.log(TAG, IO.TAG_INFO, String.format("%s %s HTTP/1.1\nHost: %s", httpConn.getRequestMethod(), function, host));
        IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "PATCHing data: "+object);

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(object);
        writer.flush();
        writer.close();
        os.close();

        return httpConn;
    }

    public static HttpURLConnection postJSON(String function, String object, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
            {
                IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "setting header: ["+header.getKey()+":"+header.getValue()+"]");
                httpConn.setRequestProperty(header.getKey(), header.getValue());
            }
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("POST");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        IO.log(TAG, IO.TAG_INFO, String.format("%s %s HTTP/1.1\nHost: %s", httpConn.getRequestMethod(), function, host));
        IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "POSTting data: "+object);

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(object);
        writer.flush();
        writer.close();
        os.close();

        return httpConn;
    }

    public static HttpURLConnection putJSON(String function, String object, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        URL urlConn = new URL(host + function);
        HttpURLConnection httpConn = (HttpURLConnection)urlConn.openConnection();
        if(headers!=null)
            for(AbstractMap.SimpleEntry<String,String> header:headers)
            {
                IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "setting header: ["+header.getKey()+":"+header.getValue()+"]");
                httpConn.setRequestProperty(header.getKey(), header.getValue());
            }
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("PUT");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);

        IO.log(TAG, IO.TAG_INFO, String.format("%s %s HTTP/1.1\nHost: %s", httpConn.getRequestMethod(), function, host));
        IO.log(RemoteComms.class.getName(), IO.TAG_INFO, "PUTting data: "+object);

        //Write to server
        OutputStream os = httpConn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        writer.write(object);
        writer.flush();
        writer.close();
        os.close();

        return httpConn;
    }

    public static void updateObjectOnServer(MVGObject object, String property)
    {
        if(SessionManager.getInstance().getActive()!=null)
        {
            if(!SessionManager.getInstance().getActive().isExpired())
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                //String id = bo.get_id();

                if(object!=null)
                {
                    headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                    headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                    try
                    {
                        HttpURLConnection connection = RemoteComms.patchJSON(object.apiEndpoint(), object.toString(), headers);
                        if(connection!=null)
                        {
                            if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                                IO.log(TAG, IO.TAG_INFO, "Successfully updated MVGObject{"+object.getClass().getName()+"}'s '" + property + "' property to ["+object.get(property)+"].");
                            else
                            {
                                String msg = IO.readStream(connection.getErrorStream());
                                /*Gson gson = new GsonBuilder().create();
                                Error error = gson.fromJson(msg, Error.class);*/
                                IO.logAndAlert("Error " +String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
                            }
                            connection.disconnect();
                        } else IO.logAndAlert("Error", "Connection to server was interrupted.", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.logAndAlert(TAG, e.getMessage(), IO.TAG_ERROR);
                    }
                } else IO.log(TAG, IO.TAG_ERROR, "Invalid MVGObject");
            } else IO.logAndAlert("Session expired", "No active sessions.", IO.TAG_ERROR);
        } else IO.logAndAlert("Error: Invalid Session", "Active Session is invalid.", IO.TAG_ERROR);
    }
}
