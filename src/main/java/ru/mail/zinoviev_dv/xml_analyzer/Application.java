package ru.mail.zinoviev_dv.xml_analyzer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;


public class Application {
    public static void main(String[] args) throws IOException {

        String dir;
        String xmlTag;

        if((args == null) || (args.length < 2)) {
            Scanner scanner = new Scanner(System.in);
            dir = getDir(scanner);
            xmlTag = getXmlTag(scanner);
            scanner.close();
        } else {
            dir = args[0];
            xmlTag = args[1];
        }

        if(dir.endsWith(".zip")){
            unzip(dir, dir.replace(".zip", "zip"));
            dir = dir.replace(".zip", "zip");
        }

        Path path = Paths.get(dir);

        if(!Files.exists(path)){
            String error = "Директория '" + path + "' не существует";
            throw new FileNotFoundException(error);
        }

        List<String> xmlFiles = filesInDir(path.toString());

        System.out.println("Files count: " + xmlFiles.size());


        HashSet<String> values = new HashSet<>();

        xmlFiles.forEach(s -> values.addAll(getValue(s, xmlTag)));

        System.out.println("Values:");

        values.forEach(System.out::println);

    }

    private static List<String> filesInDir(String dirName){
        FilenameFilter filter = (folder, name) -> name.endsWith(".xml");

        ArrayList<String> files = new ArrayList<>(
                Stream.of(Objects.requireNonNull(new File(dirName).list(filter)))
                        .map(fileName -> (dirName + "/" + fileName))
                        .toList());

        String[] folders = new File(dirName).list();

        if (folders != null) {
            Arrays.stream(folders)
                    .map(fileName -> (dirName + "/" + fileName))
                    .filter(file -> new File(file).isDirectory())
                    .forEach(elem -> files.addAll(filesInDir(elem)));
        }
        return files;
    }

    private static String getDir(Scanner scanner){
        return getValue(scanner, "Введите путь до каталога с исследуемыми файлами: ");
    }

    private static String getXmlTag(Scanner scanner){
        return getValue(scanner, "Введите исследуемый xml tag: ");
    }

    private static String getValue(Scanner scanner, String inputText){
        System.out.print(inputText);
        return scanner.nextLine();
    }

    private static Set<String> getValue(String fileName, String xmlPath){
        HashSet<String> result = new HashSet<>();
        try {
            //Для создания древовидной структуры создается объект класса DocumentBuilder
            DocumentBuilder builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();

            //Создается объект Document — он является представлением всей информации внутри XML
            Document document = builder.parse(fileName);

            //Создается список всех дочерних узлов
            NodeList childNode = document.getDocumentElement().getElementsByTagName(xmlPath);

            //В цикле выполняется поиск и присвоение значений согласно заданным условиям
            for (int i = 0; i < childNode.getLength(); i++) {
                Node element = childNode.item(i);
                if (!element.getNodeName().equals(xmlPath))
                    continue;

                if(element.hasChildNodes()){
                    NodeList child = element.getChildNodes();
                    for(int j = 0; j < child.getLength(); j++){
                        Node childElement = child.item(j);
                        if (childElement.getNodeName().equals("#text") || childElement.getNodeName().equals("#comment"))
                            continue;

                        result.add(getValue(childElement));
                    }
                }
            }
        } catch (Exception e){
            return result;
        }
        return result;
    }

    private static StringBuilder getAttributes(Node element){
        StringBuilder attributes = new StringBuilder();

        if(element.hasAttributes()){
            for(int i = 0; i < element.getAttributes().getLength(); i++) {
                if (i > 0)
                    attributes.append(";");
                attributes.append(element.getAttributes().item(i).getNodeName())
                        .append("=")
                        .append(element.getAttributes().item(i).getNodeValue());
            }
        }

        return attributes;
    }

    private static String getValue(Node element){
        StringBuilder attributes = getAttributes(element);

        StringBuilder  value = new StringBuilder();
        value.append(element.getNodeName());
        if(!attributes.isEmpty())
            value.append("(")
                    .append(attributes.toString())
                    .append(")");
        if(!element.getTextContent().isEmpty())
            value.append(" = ")
                    .append(element.getTextContent());
        return value.toString();
    }

    public static void unzip(String pathZipFilePath, String pathDestDir) throws IOException {
        Path zipFilePath = Paths.get(pathZipFilePath);
        Path destDir = Paths.get(pathDestDir);
        try (java.nio.file.FileSystem zipFileSystem = java.nio.file.FileSystems.newFileSystem(zipFilePath)) {
            Path root = zipFileSystem.getRootDirectories().iterator().next();
            try(var input = Files.walk(root)) {
                input.forEach(path -> {
                            Path destPath = Paths.get(destDir.toString(), path.toString().substring(1));
                            try {
                                if (Files.isDirectory(path)) {
                                    Files.createDirectories(destPath);
                                } else {
                                    Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);
                                    if (destPath.toString().endsWith(".zip")) {
                                        // found a zip file, try to open
                                        tryUnZip(destPath.toString(), destPath.toString().replace(".zip", "zip"));
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println(Arrays.toString(e.getStackTrace()));
                            }
                        })
                ;
            }
        }
    }

    public static void tryUnZip(String pathZipFilePath, String pathDestDir){
        try {
            unzip(pathZipFilePath, pathDestDir);
        } catch (Exception e){

            System.out.println("Ошибка в файле " + pathZipFilePath);
        }
    }

}

