
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Connection;

public class Server implements ISongInfoService{
    private static Object serv;
   
    protected Random rng      ;
    protected String name	  ;
    protected String driver   ;
    protected String url      ;
    protected String database ;
    protected String username ;
    protected String password ;
    protected ISongInfoService stub ;
    protected Connection conn;
    
    
    public void setDriver(String driverName){
        driver = driverName;
    }
    public void setUrl(String urlName){
        url = urlName;
    }
    public void setDatabase(String databaseName){
        database = databaseName;
    }
    public void setUsername(String usernameGiven){
        username = usernameGiven;
    }
    public void setPassword(String passwordGiven){
        password = passwordGiven;
    }
    public void setRng(Random rngGiven){
        rng = rngGiven;
    }
    public void setName(String nameGiven){
        name = nameGiven;
    }
    
    
    public final void initialize ()
    {
        setDriver       ("com.mysql.jdbc.Driver")        ;
        setUrl          ("jdbc:mysql://localhost:3306/") ;
        setDatabase     ("peertopeer")                   ;
        setUsername     ("root")                         ;
        setPassword     ("1234")                         ;
        setRng          (new Random ())                  ;
	setName		("peertopeer")			 ;
    }
    

	
    public Server () throws SQLException, ClassNotFoundException {
        super ();
        rng = new Random();
		initialize ();
		//more initialization stuff
                Class.forName( driver ) ;
                conn = (Connection) DriverManager.getConnection( url + database , username , password ) ;
    }
    
    public static void main(String[] args) throws RemoteException, SQLException, ClassNotFoundException {
            String codebase = "http://localhost:8080/rmi/";
            String name = "RMIServer";
            String logFile = "RMI.log";    
            Server myServer = new Server();    
    
            System.setProperty("java.rmi.server.hostname","localhost");
            System.setProperty("java.rmi.server.codebase",codebase);
            myServer.stub = (ISongInfoService) UnicastRemoteObject.exportObject( myServer , 0 ) ;
            Registry reg = LocateRegistry.createRegistry(1099);
            reg.rebind( name, myServer.stub) ;
            System.out.println("*** Welcome to RMI Server ***");
    }

    @Override
    public void updateSong(SongInfo arg) throws RemoteException {
        try 
        {
            String updateQuery = "UPDATE Songs SET "
                            + "name = ? , artist = ? , "
                            + "album = ? , genre = ? , "
                            + "year = ? where hash = ?" ;
            PreparedStatement preparedStmt = conn.prepareStatement(updateQuery) ;
            preparedStmt.setString      (1, arg.name)   ;
            preparedStmt.setString      (2, arg.artist) ;
            preparedStmt.setString      (3, arg.album)  ;
            preparedStmt.setString      (4, arg.genre)  ;
            preparedStmt.setInt         (5, arg.year)   ;
            preparedStmt.setString      (6, arg.hash)   ;
            preparedStmt.executeUpdate  ()              ;					
            preparedStmt.close          ()              ;
        }
        catch (SQLException ex) 
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }
   
    @Override
    public void addSong(SongInfo arg) throws RemoteException {
        Statement statement = null;
        try{
            statement = conn.createStatement();
            String insertQuery = "INSERT INTO Songs (hash) VALUES (" + "\"" + arg.hash + "\"" + ");";
            System.out.println(insertQuery);
            statement.executeUpdate(insertQuery);
          }catch(SQLException se){
             //Handle errors for JDBC
             se.printStackTrace();
          }catch(Exception e){
             //Handle errors for Class.forName
             e.printStackTrace();
          }    
    }

    @Override
    public SongInfo getSong(SongInfo arg) throws RemoteException {
        Statement statement = null ;
        try 
        {
            statement = conn.createStatement();
            ResultSet resultset = statement.executeQuery( "SELECT * FROM Songs WHERE hash='" + arg.hash + "';" );
            if ( !resultset.next() ){
                //System.out.println("ADD CALLED!");
                addSong(arg);
            }
            resultset = statement.executeQuery( "SELECT * FROM Songs WHERE hash='" + arg.hash + "';" );
            while( resultset.next() )
            {
                arg.id     = resultset.getInt   ("idsongs");
                arg.name   = resultset.getString("name");
                arg.artist = resultset.getString("artist");
                arg.album  = resultset.getString("album");
                arg.genre  = resultset.getString("genre");
                arg.year   = resultset.getInt   ("year");
                arg.hash   = resultset.getString("hash");
            }
            resultset.close();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arg;
        
    }
	
    /****************************************
	*										*	
    *    implement the interface methods	*
    *										*
	****************************************/
	
	
	
}
            
