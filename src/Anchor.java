/*
Data.txt path : "C:\Personal Use\Programming\Anchor\src\data.txt"
Test file path : c
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileWriter;
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

    private static int getFileContent(String filePath){

        try{
            fileLines = Files.readAllLines(Paths.get(filePath));
//            fileLines.add("big lmao new line added here!");
//            fileLines.set(0, "Haha, this line has been set differenltly!");
//            Files.write(Paths.get(filePath), lines);
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

        This method will get anchor data AND remove comment data!
        Needs to be refactored eventually.

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

//        for (Map.Entry<String, String> entry : anchorTagComments.entrySet()) {
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//        }

        try {
            Files.write(Paths.get(filePath), fileLines);
        } catch (IOException e) {
            System.out.println(e);
            return;
        }

    }

    public static int writeDataToFile(String dataPath){

//        String outputFilePath = "C:\\Personal Use\\Programming\\Anchor\\src\\data.txt";
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;

        try{
            fileWriter = new FileWriter(dataPath, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            for (Map.Entry<String, String> entry : anchorTagComments.entrySet()) {
                bufferedWriter.write(entry.getKey());
                bufferedWriter.newLine();
                bufferedWriter.write(entry.getValue());
            }

            bufferedWriter.close();

        }catch(Exception e){
            System.out.println(e);
            return -1;
        }

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

        if(command != Command.SAVE && args.length > 1){ // only save command will take a path (for now)
            System.out.println("Too many arguments for provided command!");
            return;
        }

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
            Path metaPathDir = Paths.get(metaPathString);
            String dataPathString = initDirPathString + "\\data.txt";
            Path dataPathDir = Paths.get(initDirPathString);

            if(getFileContent(args[1]) != 1){ // 2nd command line arg MUST be a path (for now)
                return;
            }

            getAnchorData(args[1]);

            if(writeDataToFile(dataPathString) == -1){
                System.out.println("Failed writing data to file!");
                return;
            }

            System.out.println("Successfully saved comments!");

        }

        System.out.println("Program exited successfully!");

    }
}