


import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ISongInfoService extends Remote {
    
    public void addSong (SongInfo arg) throws RemoteException;
    public void updateSong (SongInfo arg) throws RemoteException;
    public SongInfo getSong (SongInfo arg) throws RemoteException;
    
    
}
