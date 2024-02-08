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

                if((!anchorOption.startsWith("-") && !anchorOption.isEmpty() ) || anchorOption.length() > 2){
                    System.out.println("Skipping anchor with id : " + anchorKey + " with invalid option : " + anchorOption);
                    continue;
                }
                else{
                    System.out.println(anchorKey + "  :  " + anchorOption);
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
    public static int writeDataToFile(String dataPath, String metaPath, HashMap<String, String> anchorData){

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

            for (Map.Entry<String, String> entry : anchorData.entrySet()) {

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
            // Next 2 lines need to be tested for functionality!
            dataFileWriter.close();
            metaFileWriter.close();

        }catch(Exception e){
            System.out.println(e);
            return -1;
        }

        return 1;

    }

    private static int readStoredData(String commentId, String dataPath, String metaPath){

        File dataFile = new File(dataPath);
        File metaFile = new File(metaPath);

        if(!dataFile.exists() || !metaFile.exists()){
            System.out.println("No comment history exists!");
            return -1;
        }

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
            return -1;
        }

        temp = metaDataLine.substring(metaDataLine.indexOf(":") + 1);
        dataBounds = temp.split("-");

        if(dataBounds.length != 2){
            System.out.println("More or less than 2 data bounds when parsing metadata file!");
            return -1;
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
//        System.out.println();

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

            if(writeDataToFile(dataPathString, metaPathString, anchorData) == -1){
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

            String metaPathString = initDirPathString + "\\metadata.txt";
            String dataPathString = initDirPathString + "\\data.txt";
            readStoredData(args[1], dataPathString, metaPathString);

        }
        else{
            System.out.println("Invalid command!");
        }

    }
}