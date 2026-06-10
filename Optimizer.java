package Filer;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicLong;

public class Optimizer {
    public Path root;
    public String path;
    public ArrayList<String> trash;

    public Optimizer(String path){
        this.path = path;
        root = Paths.get(path);
        this.trash = new ArrayList<>();
    }   

    private static Boolean isEmptyDirectory(Path path) throws IOException{
        Boolean isEmpty;
        try(Stream<Path> entries = Files.list(path)){
            isEmpty = entries.findFirst().isEmpty();
        }
        return isEmpty;
    }

    private Boolean zipSecurity(Path file, Path folder) throws IOException{
        var zip = Files.getLastModifiedTime(file);
        var dir = Files.getLastModifiedTime(folder);

        // Vérifier ainsi si le dossier extrait n'est pas vide et est plus récent (donc dézippé)
        return !isEmptyDirectory(folder) && dir.compareTo(zip)>=0;
    }

    public void getDoubles() throws IOException {
        ArrayList<String> doublons = new ArrayList<>();
        Map<String,Long> sizes = new HashMap<>();
        
        Files.walk(root).forEach(file -> {

            if(Files.isRegularFile(file)){
                try{
                    if(Files.isHidden(file)) return;
                } catch(IOException e){
                    e.printStackTrace();
                }
                String name = file.getFileName().toString();
                String baseName = name.substring(0, name.length()-4);
                
                try{
                    // Vérifier si deux files sont parfaitement similaires (en  ignorant les noms)
                    // Stocker la aille du fichier et vérifier si elle correspond à un autre
                    Long size = Files.size(file);

                    if(sizes.containsValue(size)){ // Si oui : 
                        
                      for (Map.Entry<String, Long> entry : sizes.entrySet()) { // Pour chaque valeur de Map
                        
                        if (entry.getValue().equals(size)) { 
                            Path path = Paths.get(entry.getKey()); // On get la key
                            // Vérifier si les fichiers sont identiques : 
                            if (Files.mismatch(file, path) == -1) doublons.add(path.toString());
                        }

                      }
                    }

                    sizes.put(file.toString(),Files.size(file));
                } catch(IOException e){
                    e.printStackTrace();
                }

                // Vérifier si un dossier compressé a été dézipper et laissé à l'abandon.
                if(name.toLowerCase().endsWith(".zip")){
                    Path folder = file.getParent().resolve(baseName);
                    try {
                        if(Files.isDirectory(folder) && this.zipSecurity(file, folder))
                            doublons.add(file.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        trash.addAll(doublons);
    }

    public void deleteTrash(){
        final AtomicLong size = new AtomicLong(0L);
        trash.forEach(filepath -> {
            try {
                Path path = Path.of(filepath);
                size.addAndGet(Files.size(path));
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.out.println("Corbeille vidée, "+size.toString()+" octets d'économisés.");
    }

    public void optimizeDesktop(Path desktop) throws IOException{
        Files.walk(desktop)
        .filter(Files::isRegularFile)
        .filter(file -> file.getFileName().toString().endsWith(".lnk"))
        .filter(Optimizer::isBrokenShortcut)
        .forEach(file -> {
            try{
                Files.delete(file);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        });
    }
    private static boolean isBrokenShortcut(Path p) {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"powershell", "-NoProfile", "-Command",
                "(New-Object -COM WScript.Shell).CreateShortcut('" + p.toAbsolutePath() + "').TargetPath"});
            String target = new String(proc.getInputStream().readAllBytes()).trim();
            return !target.isBlank() && !Files.exists(Path.of(target));
        } catch (Exception e) { return false; }
    }
    public String toString(){
        return this.path;
    }
}
