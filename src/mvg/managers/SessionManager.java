/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import mvg.auxilary.Session;
import mvg.model.User;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ghost
 */
public class SessionManager 
{
    private static final SessionManager sess_mgr = new SessionManager();
    private HashMap<String, Session> sessions = new HashMap<>();
    private Session active;
    public static final String TAG = "SessionManager";
    
    private SessionManager(){};
    
    public static SessionManager getInstance()
    {
        return sess_mgr;
    }

    /**
     * Method to add a Session to the HashMap of sessions
     * @param session <pre>Session</pre> object to be added.
     */
    public void addSession(Session session)
    {
        if(session==null)
            return;
        //check if session being added exists in list of sessions
        Session s = getUserSession(session.getUsername());
        if(s!=null)
        {
            //if it exists in the list,update the date, session_id & ttl
            s.setDate(session.getDate());
            s.setSessionId(session.getSessionId());
            s.setTtl(session.getTtl());
            setActive(s);
        }
        else
        {
            //if it doesn't exist, add it to the list and set it as active
            sessions.put(session.getSessionId(), session);
            setActive(session);
        }
    }
    
    public User getActiveUser()
    {
        UserManager.getInstance().loadDataFromServer();
        if(UserManager.getInstance().getUsers()!=null && this.active!=null)
            return UserManager.getInstance().getUsers().get(this.active.getUsername());
        else return null;
    }
    
    public HashMap<String, Session> getSessions()
    {
        return sessions;
    }
    
    public Session getUserSession(String usr)
    {
        return sessions.get(usr);
    }
    
    public void setActive(Session session)
    {
        this.active = session;
    }
    
    public Session getActive()
    {
        return active;
    }
}
