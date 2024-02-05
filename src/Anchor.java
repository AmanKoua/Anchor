/*
Data.txt path : "C:\Personal Use\Programming\Anchor\src\data.txt"
Test file path : "C:\Personal Use\Programming\Anchor test files\WheelDeal\Transaction\TransactionController.java"
*/

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum Command{ // Placeholder for more advanced command handling
    INIT, SAVE, READ, HELP, INVALID
}

public class Anchor {

    private static List<String> fileLines;
    private static HashMap<String, String> anchorTagComments;
    private static String anchorDirPath = "";

    // Reads all file content, from specified filePath, into fileLines list
    private static int getFileContent(String filePath){

        try{
            fileLines = Files.readAllLines(Paths.get(filePath));
        }catch (Exception e){
            System.out.println(e);
            return -1; // failure
        }

        return 1; // success

    }

    /*

        Anchor comments are expected to be in the following format:
            // [Anchor.Comment.ID]
            /*
                {Comment Data}
            +/

        This method will get anchor data AND remove comment data from source code file!

     */
    private static void getAnchorData(String filePath){
        if(fileLines.isEmpty()){
            return;
        }

        anchorTagComments = new HashMap<String, String>();
        String comment = ""; // not using StringBuilder because it does not automatically append newline characters
        String anchorKey = "";

        for(int i = 0; i < fileLines.size(); i++){

            String line = fileLines.get(i);

            if(line.contains("[Anchor.")){
                comment = "";
                anchorKey = line.substring(line.indexOf("[Anchor."));
                int searchIdx = i + 1;
                boolean hasAnchorComment = fileLines.get(searchIdx).contains("/*");

                if(!hasAnchorComment){ // skip processing if no anchor comment is present
                    continue;
                }

                while(searchIdx < fileLines.size()){

                    line = fileLines.get(searchIdx);

                    if(line.contains("*/")){
                        fileLines.remove(searchIdx);
                        break;
                    }

                    if(line.contains("/*")){
                        fileLines.remove(searchIdx);
                        continue;
                    }

                    comment += line.trim();

                    fileLines.remove(searchIdx);
                    comment += "\n";
                }
                anchorTagComments.put(anchorKey, comment.toString());
            }

        }

        try {
            Files.write(Paths.get(filePath), fileLines);
        } catch (IOException e) {
            System.out.println(e);
            return;
        }

    }

    // get number of newline characters in a string
    private static int getNewlineCount(String data){

        int count = 0;

        for(int i = 0; i < data.length(); i++){
            if(data.charAt(i) == '\n'){
                count++;
            }
        }

        return count;

    }

    // write comment data, and generated metadata, to file
    public static int writeDataToFile(String dataPath, String metaPath){

        FileWriter dataFileWriter;
        FileWriter metaFileWriter;
        BufferedWriter dataBufferedWriter;
        BufferedWriter metaBufferedWriter;
        int lineCount = 0;

        try{
            dataFileWriter = new FileWriter(dataPath, true);
            dataBufferedWriter = new BufferedWriter(dataFileWriter);
            metaFileWriter = new FileWriter(metaPath, true);
            metaBufferedWriter = new BufferedWriter(metaFileWriter);
        }catch(IOException e){
            System.out.println("Failed initializing data / metadata files!");
            System.out.println(e);
            return -1;
        }

        try{

            for (Map.Entry<String, String> entry : anchorTagComments.entrySet()) {

                // write metadata for efficient data traversal upon reading comments
                metaBufferedWriter.write(entry.getKey());
                metaBufferedWriter.write(":");
                metaBufferedWriter.write(String.valueOf(lineCount + 1));
                metaBufferedWriter.write("-");
                metaBufferedWriter.write(String.valueOf(getNewlineCount(entry.getValue()) + (lineCount + 2)));
                metaBufferedWriter.newLine();

                lineCount = getNewlineCount(entry.getValue()) + (lineCount + 1);

                // Write data to data file
                dataBufferedWriter.write(entry.getKey());
                dataBufferedWriter.newLine();
                dataBufferedWriter.write(entry.getValue());
            }

            dataBufferedWriter.close();
            metaBufferedWriter.close();

        }catch(Exception e){
            System.out.println(e);
            return -1;
        }

        return 1;

    }

    private static int readStoredData(String commentId, String dataPath, String metaPath){

        BufferedReader metaReader = null;
        BufferedReader dataReader = null;
        String temp;
        String commentData = "";
        String fullCommentId = "[Anchor." + commentId + "]";
        String metaDataLine = ""; // init
        String commentDataLine = ""; // init
        String[] dataBounds;
        boolean isCommentIdInMetadata = false;
        int lineCount = 0;

//        System.out.println("full comment id: " + fullCommentId);

        try{
            metaReader = new BufferedReader(new FileReader(metaPath));
            dataReader = new BufferedReader(new FileReader(dataPath));
        } catch(Exception e){
            System.out.println("Failed to open metadata / data files for reading!");
        }


        while(metaDataLine != null){
            
            if(metaDataLine.contains(fullCommentId)){
                isCommentIdInMetadata = true;
                break;
            }

            try{
                metaDataLine = metaReader.readLine();
            }
            catch(Exception e){
                System.out.println(e);
            }

        }

        if(!isCommentIdInMetadata){
            System.out.println("Comment ID was not found!");
            return 1;
        }

        temp = metaDataLine.substring(metaDataLine.indexOf(":") + 1);
        dataBounds = temp.split("-");

        if(dataBounds.length != 2){
            System.out.println("More or less than 2 data bounds when parsing metadata file!");
            return 1;
        }

        System.out.println();

        while(commentDataLine != null){

            if(lineCount >= Integer.parseInt(dataBounds[1].trim())){
                break;
            }

            if(lineCount > Integer.parseInt(dataBounds[0].trim())){
                commentData += commentDataLine + "\n";
            }

            try{
                commentDataLine = dataReader.readLine();
            }
            catch(Exception e){
                System.out.println(e);
            }

            lineCount++;

        }

        System.out.println(commentData);
        System.out.println();

        return 1;
    }

    public static Set<String> getDirsInCurrentDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static boolean isRootDirInitialized(Set<String> dirs){
        for(String dir : dirs){
            if(dir.equals(".anchor")){
                return true;
            }
        }
        return false;
    }

    // Parses command arg and returns corresponding enum value
    public static Command parseCommand(String command){
        switch(command.toLowerCase()){
            case "init":
                return Command.INIT;
            case "save":
                return Command.SAVE;
            case "read":
                return Command.READ;
            case "help":
                return Command.HELP;
            default:
                return Command.INVALID;
        }
    }

    public static void main(String[] args) {

        if(args.length == 0){
            System.out.println("Anchor is a command line tool for organizing and managing comments made in source code files.\nUse the \"help\" command to learn more!");
            return;
        }
        else if (args.length > 2){
            System.out.println("Too many arguments! Maximum of 2 expected.");
            return;
        }

        Command command = parseCommand(args[0]);
        String filePath = "";
        Set<String> dirs;
        String currentDir = System.getProperty("user.dir");
        String initDirPathString = currentDir + "\\.anchor";
        Path initDirPath = Paths.get(initDirPathString);

        // TODO : implement command arg limiting!
//        if((command == Command.SAVE && args.length > 1)){ // only save command will take a path (for now)
//            System.out.println("Too many arguments for provided command!");
//            return;
//        }
//        else if((command == Command.READ && args.length > 2)){
//            System.out.println("Too many arguments for provided command!");
//            return;
//        }

        // Placeholder method of handling commands.
        if(command == Command.HELP){
            System.out.println("Supported commands are: init, save, read, and help");
            return;
        }
        else if(command == Command.INIT){

            try {
                dirs = getDirsInCurrentDir(currentDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(isRootDirInitialized(dirs)){
                System.out.println("This directory is already initialized!");
                return;
            }

            try {
                Files.createDirectories(initDirPath);
            } catch(Exception e){
                System.out.println(e);
                return;
            }

            System.out.println("Successfully initialized anchor directory!");

        }
        else if (command == Command.SAVE){ // Change later on to scan entire file structure for provided source file type

            try {
                dirs = getDirsInCurrentDir(currentDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(!isRootDirInitialized(dirs)){
                System.out.println("This directory is not initialized! Initialize this directory with the command \"anchor init\" ");
                return;
            }

            String metaPathString = initDirPathString + "\\metadata.txt";
            String dataPathString = initDirPathString + "\\data.txt";

            if(getFileContent(args[1]) != 1){ // 2nd command line arg MUST be a path (for now)
                return;
            }

            getAnchorData(args[1]);

            if(writeDataToFile(dataPathString, metaPathString) == -1){
                System.out.println("Failed writing data to file!");
                return;
            }

            System.out.println("Successfully saved comments!");

        }
        else if (command == Command.READ){

            try {
                dirs = getDirsInCurrentDir(currentDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(!isRootDirInitialized(dirs)){
                System.out.println("This directory is not initialized! Initialize this directory with the command \"anchor init\" ");
                return;
            }

            String metaPathString = initDirPathString + "\\metadata.txt";
            String dataPathString = initDirPathString + "\\data.txt";
            readStoredData(args[1], dataPathString, metaPathString);

        }

        System.out.println("Program exited successfully!");

    }
}