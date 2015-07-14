import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.*;

/**
 *
 * @author Burak
 */
public class Client {
    protected static MessageDigest md;
    protected static Random rng;
    protected static ISongInfoService stub;
    
    public static byte[] fillContentFile( /*byte[] selectedFileContent*/ File tempFile) throws FileNotFoundException, IOException {
        int inputFileLength = (int)tempFile.length();
        FileInputStream streamContent = new FileInputStream(tempFile);
        byte[] selectedFileContent = new byte[inputFileLength];
        
        streamContent.read(selectedFileContent);
        streamContent.close();
        return selectedFileContent;
    }
    
    public static String computeHash( byte[] selectedFileContent ){
        byte[] digest = null;
        digest = md.digest(selectedFileContent);
        String hash = "";
    //    byte [] digest = md.digest (selectedFileContent);
        BigInteger bi = new BigInteger (1, digest);
        hash = bi.toString(16);
        return hash;
    }
    
    public static int serializeSong(Song selectedSong, String serializeFileName) throws FileNotFoundException, IOException{
        FileOutputStream fos = new FileOutputStream( serializeFileName );
        try (ObjectOutputStream oos = new ObjectOutputStream( fos )) {
            oos.writeObject( selectedSong ) ;
        }
        catch(Exception e){
            return 0;
        }
        return 1; 
    }
    
    public static Song deserializeSong(String serializeFileName) throws FileNotFoundException, IOException{
        Song deserialSong = null;
        FileInputStream fis = new FileInputStream( "inputs/" + serializeFileName );
        try (ObjectInputStream ois = new ObjectInputStream( fis )) {
            deserialSong = (Song) ois.readObject() ;
        }
        catch(Exception e){
            return null;
        }
        return deserialSong; 
    }
    
    public static String checkConstraint(String tempStr, int consLen ){
        return ( tempStr.length() > consLen ? tempStr.substring(0, consLen) : tempStr );
    }
    
    public static void main(String[] args) throws RemoteException, NotBoundException, FileNotFoundException, IOException, ClassNotFoundException {
        Client myClient = new Client();
        Scanner in = new Scanner(System.in);
        String command = "";
        String selectedFile = "";
        String pathSelectedFile = "";
        int selectedIndex = -1;
        File tempFile = null;
        //FileInputStream inputFile = null;
        byte[] selectedFileContent = null;
        String selectedFileHash = "";
        String serializeFileName = "";
        String toUpdateSerName = "";
        Song deserialSong = null;
        String tempName = "";
        String tempArtist = "";
        String tempGenre = "";
        String tempAlbum = "";
        String tempYear = "";
        String readFileName = ""; 
        boolean fileExist = false;
        
        System.out.println("*** Welcome to RMI Client ***");
        System.out.println("Please type your command without any space");
        System.out.println("Available commands are: get, update, read, metadata, exit");
        
        while(true){
            System.out.println("Type Your Command:");
            command = in.nextLine();
            
            if ( command.equals("get") ){
                System.out.println("Write the file name without .audio extension");
                selectedFile = in.nextLine();
                
                File dir = new File ("inputs");
                File [] list = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        // get last index for '.' char
                        int lastIndex = name.lastIndexOf('.');
                        // get extension
                        if (lastIndex > 0) {
                            String str = name.substring(lastIndex);
                            // match extension
                            if(str.equals(".audio"))
                            {
                            return true;
                            } 
                        }
                        return false;
                    }});
                for (int i=0; i<list.length; i++)
                {
                    if ( list[i].getName().toLowerCase().equals( (selectedFile + ".audio").toLowerCase() ) )
                    {
                        //System.out.println("FOUND!");
                        pathSelectedFile = "inputs/" + selectedFile + ".audio";
                        tempFile = new File(pathSelectedFile);
                        break;
                    }   
                }    

                if ( tempFile != null  )
                {
                    System.out.println("File is found!");
                    
                    selectedFileContent = fillContentFile(tempFile);
                    selectedFileHash = computeHash( selectedFileContent );
                    
                    SongInfo selectedFileInfo = new SongInfo();
                    selectedFileInfo.hash = selectedFileHash;
                    selectedFileInfo = myClient.stub.getSong(selectedFileInfo);
                    
                    Song selectedSong = new Song();
                    selectedSong.data = selectedFileContent;
                    selectedSong.info = selectedFileInfo;
                    printSongInfo(selectedSong.info);
 
                    System.out.println("Your song is fetched.");
                    System.out.println("Please write the name of serialize object to be saved: ");
                    serializeFileName = in.nextLine();
                    serializeFileName = "inputs/" + serializeFileName + ".ser"; 
                    int resultSerial = serializeSong(selectedSong, serializeFileName);
                    if ( resultSerial == 1 )
                    {
                        System.out.println("Your serial object is created on " + serializeFileName );
                    }
                    else
                    {
                        System.out.println("Your serial object could not be created! ");
                    }
                }
                else
                {
                    System.out.println("File is NOT found!");
                }
            }
            
            else if ( command.equals("update") ){
                System.out.println("Please give the serialized file name to update without .ser extension:");
                toUpdateSerName = in.nextLine();
                toUpdateSerName += ".ser";
                fileExist = new File("inputs", toUpdateSerName ).exists();
                if ( fileExist ){
                    //System.out.println("File exist!");
                    deserialSong = deserializeSong( toUpdateSerName );
                    
                    if ( deserialSong == null ){
                        System.out.println("There is an error on deserializing");
                    }
                    else
                    {    
                        System.out.println("You selected the song: ");
                        printSongInfo( deserialSong.info );
                        //System.out.println("NOW CHANGE THE PARAMETERS!!!");
                        System.out.println("Now, type the changed parameters: ");
                        System.out.println("Enter Name: ");
                        tempName = in.nextLine();
                        if ( tempName.equals("") )
                        {
                            tempName = deserialSong.info.name;
                        }    
                        tempName = checkConstraint( tempName, 45 );
                        
                        System.out.println("Enter Artist: ");
                        tempArtist = in.nextLine();
                        if ( tempArtist.equals("") )
                        {
                            tempArtist = deserialSong.info.artist;
                        } 
                        tempArtist = checkConstraint( tempArtist, 45 );
                        
                        System.out.println("Enter Album: ");
                        tempAlbum = in.nextLine();
                        if ( tempAlbum.equals("") )
                        {
                            tempAlbum = deserialSong.info.album;
                        }
                        tempAlbum = checkConstraint( tempAlbum, 45 );
                        
                        System.out.println("Enter Genre: ");
                        tempGenre = in.nextLine();
                        if ( tempGenre.equals("") )
                        {
                            tempGenre = deserialSong.info.genre;
                        }
                        tempGenre = checkConstraint( tempGenre, 45 );
                        
                        System.out.println("Enter Year: ");
                        tempYear = in.nextLine();
                        if ( tempYear.equals("") )
                        {
                           tempYear = Integer.toString(deserialSong.info.year);
                        }
                        tempYear = checkConstraint( tempYear, 5 );
                    
                        deserialSong.info.name = tempName;
                        deserialSong.info.artist = tempArtist;
                        deserialSong.info.album = tempAlbum;
                        deserialSong.info.genre = tempGenre;
                        deserialSong.info.year = Integer.parseInt(tempYear);
                        
                        myClient.stub.updateSong(deserialSong.info);


                        toUpdateSerName = "inputs/" + toUpdateSerName; 
                        int resultSerial1 = serializeSong(deserialSong, toUpdateSerName);
                        if ( resultSerial1 == 1 ){
                            System.out.println("Your serial object is changed on " + toUpdateSerName );
                        }
                        else{
                            System.out.println("Your serial object could not be created! ");
                        }   
                    }    
                }
                else{
                    System.out.println("The file you entered does not exist!");
                }  
            }
            
            else if ( command.equals("read") ){
                System.out.println("Enter the name of serial file to read: -without .ser extension");
                readFileName = in.nextLine();
                if ( readFileName.equals("*") ){
                    //System.out.println("IF!!!");
                    File dir = new File ("inputs");
                    File [] list = dir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            // get last index for '.' char
                            int lastIndex = name.lastIndexOf('.');
                            // get extension
                            if (lastIndex > 0) {
                                String str = name.substring(lastIndex);
                                // match extension
                                if(str.equals(".ser"))
                                {
                                    return true;
                                } 
                            }
                            return false;
                        }});
                    if (list.length <= 0)
                    {
                        System.out.println("There is no any serial file(.ser).");
                    }    
                    else{
                        for ( int i=0; i<list.length; i++ )
                        {
                            String nameReadFile = list[i].getName();
                            Song readSongAll = deserializeSong( nameReadFile );
                            printSongInfo( readSongAll.info );
                        }    
                    }    
                }
                else{
                    //System.out.println("ELSE!!!");
                    readFileName += ".ser";
                    Song newDeserial = null;
                    fileExist = new File("inputs", readFileName ).exists();
                    if ( fileExist ){
                        Song readSong = deserializeSong( readFileName );
                        printSongInfo( readSong.info );
                    }
                    else{
                        System.out.println("There is no file with that name!");
                    }    
                }

            }
            
            else if ( command.equals("metadata") ){
                System.out.println("Enter filename to find metadata: ");
                readFileName = in.nextLine();
                fileExist = new File("inputs", readFileName ).exists();
                if ( fileExist ){
                    Path file = Paths.get("inputs/" + readFileName);
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

                    System.out.println("Creation Time: " + attr.creationTime());
                    System.out.println("Last Access Time: " + attr.lastAccessTime());
                    System.out.println("Last Modified Time: " + attr.lastModifiedTime());
                    System.out.println("Size: " + attr.size());
                }
                else{
                    System.out.println("Entered file is not found.");
                    System.out.println("Please also use the extensions.");
                }
            
            }
            
            else if ( command.equals("exit") ){
                System.out.println("Exit!");
                break;
            }
            
            else{
                System.out.println("Your command is not recognized!");
                System.out.println("Be sure that there is no space character.");
            }
        }
        
            
            
            
        /*
                 File dir = new File("inputs");
                File [] list = dir.listFiles(new FilenameFilter() 
                {
                    @Override
                    public boolean accept(File dir, String name) 
                    {
                        // get last index for '.' char
                        int lastIndex = name.lastIndexOf('.');
                        // get extension
                        if (lastIndex > 0) 
                        {
                            String str = name.substring(lastIndex);
                            // match extension
                            if(str.equals(".audio"))
                            {
                               return true;
                            } 
                        }
                        return false;
                    }
                });*/
            /*
*/
    }
                  //  }
                		//************************************************
		//get input.audio: 
                /*
                //read the input file. 
                byte [] inputContent = new byte [<input size>];

                //calculate hash
                byte [] digest = md.digest (inputContent);
                BigInteger bi = new BigInteger (1, digest);
                String hash = bi.toString(16);

                //get the song info using RMI
                SongInfo inf = <RMICall>;
                //create new song
                Song s = new Song (inf, inputContent);
                //ask user for serialized file name
                //serialize s to inputted file name
                
                
            }
            else if ( command.equals("exit") )
            {
                System.out.println("Exit: ");
            
            }
            else{
                System.out.println("Else");
            
            }
        }
            
        
    
	//Code logic goes here
    
		
	//get input
	//process command
		//************************************************
		//get input.audio: 
                /*
                //read the input file. 
                byte [] inputContent = new byte [<input size>];

                //calculate hash
                byte [] digest = md.digest (inputContent);
                BigInteger bi = new BigInteger (1, digest);
                String hash = bi.toString(16);

                //get the song info using RMI
                SongInfo inf = <RMICall>;
                //create new song
                Song s = new Song (inf, inputContent);
                //ask user for serialized file name
                //serialize s to inputted file name
        */
        //**************************************************
        //read input.ser | *:

                //if input.ser is supplied
                        //read the input.ser to an object
                        //print the content of input.ser 's SongInfo

                //if * is supplied
                        //print all the .ser files
                /*
                File dir = new File (System.getProperty("user.dir"));
                File [] list = dir.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                                                        // get last index for '.' char
                        int lastIndex = name.lastIndexOf('.');
                                                        // get extension
                        if (lastIndex > 0) {
                                                                String str = name.substring(lastIndex);
                                                                // match extension
                                                                if(str.equals(".ser"))
                                                                {
                                                                   return true;
                                                                } 
                                                        }
                                                        return false;*/
                  //  }
        //****************************************************
        //update input.ser
                        //deserialize the input.ser to an object
                        //change related fields in the SongInfo using interface or command arguments
                        //update the song in the database using RMI
                        //overwrite the serialized file with the new info
//	});
    
    public static void printSongInfo( SongInfo selectedFileInfo ){
        System.out.println("Song Information: ");
        System.out.println("ID: " +     selectedFileInfo.id);
        System.out.println("Name: " +   selectedFileInfo.name);
        System.out.println("Artist: " + selectedFileInfo.artist);
        System.out.println("Album: " +  selectedFileInfo.album);
        System.out.println("Genre: " +  selectedFileInfo.genre);
        System.out.println("Year: " +   selectedFileInfo.year);
        System.out.println("Hash: " +   selectedFileInfo.hash);
    
    }
    
    public Client() throws RemoteException, NotBoundException {
        rng = new Random();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        //connect to RMI repository. Pay attention to the bound service name in the server.
		//(do not forget to start the repository service)
        String host = "localhost" ;
        String name = "RMIServer" ;

        Registry registry = LocateRegistry.getRegistry( host )     ;
        stub     = (ISongInfoService) registry.lookup( name ) ;
    }
   
    
}
