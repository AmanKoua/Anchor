import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum Command{
    INIT, SAVE, READ, HELP, INVALID
}

public class Anchor {

    // Reads all file content, from specified filePath, into fileLines list
    private static List<String> getFileContent(String filePath){

        List<String> fileContent;
        File file = new File(filePath);

        if(!file.exists()){
            System.out.println("Provided file path does not exist!");
            return null;
        }

        try{
            fileContent = Files.readAllLines(Paths.get(filePath));
        }catch (Exception e){
            System.out.println(e);
            return null; // failure
        }

        return fileContent; // success

    }

    /*

        Anchor comments are expected to be in the following format:
            // [Anchor.Comment.ID]
            /*
                {Comment Data}
            +/

        This method will get anchor data AND remove comment data from source code file!

     */
    private static void extractAnchorComments(List<String> fileContent, HashMap<String, String> anchorData, HashMap<String, String> anchorOptions){
        if(fileContent.isEmpty()){
            return;
        }

        String comment = ""; // not using StringBuilder because it does not automatically append newline characters
        String anchorKey = "";
        String anchorOption = "";

        for(int i = 0; i < fileContent.size(); i++){

            String line = fileContent.get(i);

            if(line.contains("[Anchor.")){
                comment = "";
                anchorKey = line.substring(line.indexOf("[Anchor."), line.indexOf("]") + 1);
                anchorOption = line.substring(line.indexOf("]") + 1).trim();

                fileContent.set(i, "// " + anchorKey); // remove option flags from comments if present

                if((!anchorOption.startsWith("-") && !anchorOption.isEmpty() ) || anchorOption.length() > 2){
                    System.out.println("Skipping anchor with id : " + anchorKey + " with invalid option : " + anchorOption);
                    continue;
                }
                else{
//                    System.out.println(anchorKey + "  :  " + anchorOption);
                    anchorOptions.put(anchorKey, anchorOption);
                }

                int searchIdx = i + 1;
                boolean hasAnchorComment = fileContent.get(searchIdx).contains("/*");

                if(!hasAnchorComment){ // skip processing if no anchor comment is present
                    continue;
                }

                while(searchIdx < fileContent.size()){

                    // file lines are removed as a means to iterate through the file lines.

                    line = fileContent.get(searchIdx);

                    if(line.contains("*/")){
                        fileContent.remove(searchIdx);
                        break;
                    }

                    if(line.contains("/*")){
                        fileContent.remove(searchIdx);
                        continue;
                    }

                    comment += line.trim();
                    comment += "\n";

                    fileContent.remove(searchIdx);
                }
                anchorData.put(anchorKey, comment.toString());
            }

        }

    }

    public static int writeUpdatedFile(String filePath, List<String> fileContent){
        try {
            Files.write(Paths.get(filePath), fileContent); // save file with removed anchor comments
        } catch (IOException e) {
            System.out.println(e);
            return -1;
        }
        return 1;
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
    public static int writeDataToFile(String initDirPathString, HashMap<String, String> anchorData, HashMap<String, String> anchorOptions){

        final String dataDirPathString = initDirPathString + "\\data";

        if(!Files.exists(Path.of(dataDirPathString))){
            try{
                Files.createDirectory(Path.of(dataDirPathString));
            } catch(Exception e){
                System.out.println("Failed to create data directory!");
                System.out.println(e);
                return -1;
            }
        }

        boolean isSkipIteration = false;
        FileWriter dataFileWriter;
        BufferedWriter dataBufferedWriter;
        String commentPathString;
        String commentOption;
        Set<String> anchorCommentIds = anchorData.keySet();
        Set<String> anchorOptionsCommentsIdx = anchorOptions.keySet();

        // handle comments which have flags but no data
        for(String commentId : anchorOptionsCommentsIdx){

            if(anchorData.get(commentId) != null){
                continue;
            }

            commentPathString = dataDirPathString + "\\" + commentId + ".txt";
            commentOption = anchorOptions.get(commentId);
            commentOption = commentOption.trim();

            if(commentOption == null || commentOption.isEmpty()){
                continue;
            }

            commentOption = commentOption.substring(1);

            if(commentOption.equals("a")){
                continue;
            }
            else if(commentOption.equals("u")){ // an update without content means that the user wants to update the comment data to nothing (delete).
                try{
                    Files.deleteIfExists(Path.of(commentPathString));
                } catch(Exception e){
                    System.out.println("Error deleting data file!");
                    return -1;
                }
            }
            else if(commentOption.equals("r")){
                try{
                    Files.deleteIfExists(Path.of(commentPathString));
                } catch(Exception e){
                    System.out.println("Error deleting data file!");
                    return -1;
                }
            }
            else{
                System.out.println("Invalid option provided for : " + commentId);
                continue;
            }

        }

        // handle all commentIds
        for(String commentId : anchorCommentIds){

            isSkipIteration = false;
            commentPathString = dataDirPathString + "\\" + commentId + ".txt";
            commentOption = anchorOptions.get(commentId);

            if(commentOption != null){
                commentOption = commentOption.trim();
                if(commentOption.isEmpty()){
                    commentOption = null;
                }
            }

            // handle comment flags
            if(commentOption != null){

                commentOption = commentOption.substring(1);

                switch(commentOption){
                    case "u": // update
                        try{
                            Files.deleteIfExists(Path.of(commentPathString));
                        } catch(Exception e){
                            System.out.println("Error deleting data file!");
                            return -1;
                        }
                        break;
                    case "a": // append
                        // no action required to append to file
                        break;
                    case "r": // remove
                        try{
                            Files.deleteIfExists(Path.of(commentPathString));
                            isSkipIteration = true;
                        } catch(Exception e){
                            System.out.println("Error deleting data file!");
                            return -1;
                        }
                        break;
                    default:
                        System.out.println("Invalid option provided for : " + commentId);
                        isSkipIteration = true;
                }

                if(isSkipIteration){
                    continue;
                }

            }

            try{

                if(!Files.exists(Path.of(commentPathString))){
                    Files.createFile(Path.of(commentPathString));
                }

                dataFileWriter = new FileWriter(commentPathString, true);
                dataBufferedWriter = new BufferedWriter(dataFileWriter);
            }catch(IOException e){
                System.out.println("Failed initializing data file(s)!");
                System.out.println(e);
                return -1;
            }

            try{
                dataBufferedWriter.write(anchorData.get(commentId));
                dataBufferedWriter.close();
                dataFileWriter.close();
            } catch(Exception e){
                System.out.println("Failed writing comment data to file!");
                System.out.println(e);
                return -1;
            }

        }

        return 1;

    }

    private static int readStoredData(String commentId, String initDirPathString){

        final String commentPathString = initDirPathString + "\\data\\" + "[Anchor." + commentId + "].txt";

        if(!Files.exists(Path.of(commentPathString))){
            System.out.println("No comment data file exists!");
            return -1;
        }

        try{
            FileReader commentFileReader = new FileReader(commentPathString);
            BufferedReader commentBufferedReader = new BufferedReader(commentFileReader);

            String commentData = commentBufferedReader.readLine();

            while(commentData != null){
                System.out.println(commentData);
                commentData = commentBufferedReader.readLine();
            }


        } catch (Exception e){
            System.out.println("Error reading from comment data file!");
            return -1;
        }

        return 1;
    }

    private static List<String> getTargetFilePaths(String initDirPath){

        ArrayList<String> result = new ArrayList<String>();
        HashMap<String, String> configData = getConfigData(initDirPath);
        String targetDirPath = "";
        String targetExtension = "";

        if(configData == null){
            System.out.println("Config data is null!");
            return null;
        }

        if(!configData.containsKey("targetDir")){
            System.out.println("Configuration does not contain targetDir key!");
            return null;
        }

        if(!configData.containsKey("targetExtension")){
            System.out.println("Configuration does not contain targetExtension key!");
            return null;
        }

        targetDirPath = configData.get("targetDir");
        targetExtension = configData.get("targetExtension");

        getTargetFilePathsHelper(result, targetDirPath, targetExtension);

        return result;

    }

    public static void getTargetFilePathsHelper(ArrayList<String> result, String path, String targetExtension){

        Set<String> dirs;
        Set<String> files;

        try{
            dirs = getDirPathsInCurrentDir(path);
            files = getFilesPathsInCurrentDir(path);

            for(String file : files){
                if(file.endsWith(targetExtension)){
                    result.add(file);
                }
            }

            for(String dir: dirs){
                getTargetFilePathsHelper(result, dir, targetExtension);
            }

        } catch(Exception e){
            System.out.println(e);
        }

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

    public static Set<String> getDirPathsInCurrentDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> Files.isDirectory(file))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static Set<String> getFilesPathsInCurrentDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static int initConfigFile(String targetDir, String initDirPathString, String targetExtension){

        String configPathString = initDirPathString + "\\config.txt";
        Path configPath = Path.of(initDirPathString + "\\config.txt");

        try{
            Files.createFile(configPath);
        } catch(Exception e){
            System.out.println("Failed creating configuration file!");
            System.out.println(e);
            return -1;
        }

        try{
            FileWriter configFileWriter = new FileWriter(configPathString);
            BufferedWriter configFileBufferedWriter = new BufferedWriter(configFileWriter);
            configFileBufferedWriter.write("targetDir=" + targetDir);
            configFileBufferedWriter.newLine();
            configFileBufferedWriter.write("targetExtension=" + targetExtension);
            configFileBufferedWriter.close();
            configFileWriter.close();
        } catch(Exception e){
            System.out.println("Error writing data to config file!");
            return -1;
        }

        return 1;

    }

    public static HashMap<String, String> getConfigData(String initDirPath){

        File temp = new File(initDirPath);
        HashMap<String, String> result = new HashMap<String, String>();

        if(!temp.exists() || !temp.isDirectory()){
            System.out.println("Cannot get config data because init dir does not exist!");
            return null;
        }

        try{
            FileReader configReader = new FileReader(initDirPath + "\\config.txt");
            BufferedReader configBufferedReader = new BufferedReader(configReader);
            String configString = "";

            while(configString != null){
                configString = configBufferedReader.readLine();

                if(configString == null){
                    break;
                }

                if(configString.contains("=")){
                    String[] splitResult = configString.split("=");

                    if(splitResult.length != 2){
                        System.out.println("Error parsing line in config file!");
                        return null;
                    }
                    else{
                        result.put(splitResult[0], splitResult[1]);
                    }

                }

            }

        } catch(Exception e){
            System.out.println("Error reading config data from config file!");
            System.out.println(e);
            return null;
        }

        return result;

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
        String currentDir = System.getProperty("user.dir"); // gets directory command is being run from
        String initDirPathString = currentDir + "\\.anchor";
        Path initDirPath = Paths.get(initDirPathString);

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

            System.out.println("Please enter the absolute path for the root of the target directory: ");

            Scanner inputScanner = new Scanner(System.in);
            String targetDirPath = inputScanner.nextLine();
            File temp = new File(targetDirPath);

            System.out.println("Please enter the file extension of the source code file(s) to be tracked by anchor: ");
            String targetExtension = inputScanner.nextLine();

            if(!temp.exists()){
                System.out.println("Provided target directory does not exist!");
                System.out.println("Initialization cancelled!");
                return;
            }

            if(!temp.isDirectory()){
                System.out.println("Provided path is not a directory!");
                System.out.println("Initialization cancelled!");
                return;
            }

            try {
                Files.createDirectories(initDirPath);
            } catch(Exception e){
                System.out.println(e);
                return;
            }

            if(initConfigFile(targetDirPath, initDirPathString, targetExtension) == -1){
                return;
            }

            System.out.println("Successfully initialized anchor directory!");

        }
        else if (command == Command.SAVE){

            if(args.length != 1){
                System.out.println("Expected no extra arguments for \"save\" command!");
                return;
            }

            try {
                dirs = getDirsInCurrentDir(currentDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(!isRootDirInitialized(dirs)){
                System.out.println("This directory is not initialized! Initialize this directory with the command \"anchor init\" ");
                return;
            }

            List<String> targetFiles = getTargetFilePaths(initDirPathString);
            String metaPathString = initDirPathString + "\\metadata.txt";
            String dataPathString = initDirPathString + "\\data.txt";
            List<String> fileContent;
            HashMap<String, String> anchorData = new HashMap<String, String>();
            HashMap<String, String> anchorOptions = new HashMap<String, String>();

            for(String targetFile : targetFiles){

                fileContent = getFileContent(targetFile);

                if(fileContent == null || fileContent.isEmpty()){
                    continue;
                }

                extractAnchorComments(fileContent, anchorData, anchorOptions);

                if(writeUpdatedFile(targetFile, fileContent) != 1){
                    System.out.println("Failed updating source code file after anchor comments were extracted!");
                    return;
                }

            }

            if(writeDataToFile(initDirPathString, anchorData, anchorOptions) == -1){
                System.out.println("Failed writing data to file!");
                return;
            }

            System.out.println("Successfully saved comments!");

        }
        else if (command == Command.READ){

            if(args.length != 2){
                System.out.println("Expected 2 arguments!");
                return;
            }

            try {
                dirs = getDirsInCurrentDir(currentDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(!isRootDirInitialized(dirs)){
                System.out.println("This directory is not initialized! Initialize this directory with the command \"anchor init\" ");
                return;
            }

            readStoredData(args[1], initDirPathString);

        }
        else{
            System.out.println("Invalid command!");
        }

    }
}