package org.bbop.apollo.tools.seq.search.blat;

import org.bbop.apollo.tools.seq.search.AlignmentParsingException;
import org.bbop.apollo.tools.seq.search.SequenceSearchTool;
import org.bbop.apollo.tools.seq.search.SequenceSearchToolException;
import org.bbop.apollo.tools.seq.search.blast.TabDelimittedAlignment;
import org.gmod.gbol.bioObject.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BlatCommandLine extends SequenceSearchTool {

    private String blatBin;
    private String tmpDir;
    private String database;
    private String blatUserOptions;
    private boolean removeTmpDir;
    protected String [] blatOptions;
    
    @Override
    public void parseConfiguration(InputStream config) throws SequenceSearchToolException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(config);
            Node blatBinNode = doc.getElementsByTagName("blat_bin").item(0);
            if (blatBinNode == null) {
                throw new SequenceSearchToolException("Configuration missing required 'blat_bin' element");
            }
            blatBin = blatBinNode.getTextContent();
            Node tmpDirNode = doc.getElementsByTagName("tmp_dir").item(0);
            if (tmpDirNode == null) {
                throw new SequenceSearchToolException("Configuration missing required 'tmp_dir' element");
            }
            tmpDir = tmpDirNode.getTextContent();
            Node databaseNode = doc.getElementsByTagName("database").item(0);
            if (databaseNode == null) {
                throw new SequenceSearchToolException("Configuration missing required 'database' element");
            }
            database = databaseNode.getTextContent();
            Node optionsNode = doc.getElementsByTagName("blat_options").item(0);
            if (optionsNode != null) {
                blatUserOptions = optionsNode.getTextContent();
            }
            Node removeTmpDirNode = doc.getElementsByTagName("remove_tmp_dir").item(0);
            removeTmpDir = removeTmpDirNode != null ? Boolean.parseBoolean(removeTmpDirNode.getTextContent()) : false;
        } catch (Exception e) {
            throw new SequenceSearchToolException("Error parsing configuration: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<Match> search(String uniqueToken, String query, String databaseId) throws SequenceSearchToolException {
        File dir = null;
        try {
            dir = createTmpDir(uniqueToken);
            return runSearch(dir, query, databaseId);
        }
        catch (IOException e) {
            throw new SequenceSearchToolException("Error running search: " + e.getMessage(), e);
        }
        catch (AlignmentParsingException e) {
            throw new SequenceSearchToolException("Alignment parsing error: " + e.getMessage(), e);
        }
        catch (InterruptedException e) {
            throw new SequenceSearchToolException("Error running search: " + e.getMessage(), e);
        }
        finally {
            if (removeTmpDir && dir!=null) {
                deleteTmpDir(dir);
            }
        }
    }
    
    private Collection<Match> runSearch(File dir, String query, String databaseId) throws IOException, AlignmentParsingException, InterruptedException {
        PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(dir + "/search.log")));
        String queryArg = createQueryFasta(dir, query);
        String databaseArg = database + (databaseId != null ? ":" + databaseId : "");
        String outputArg = dir.getAbsolutePath() + "/results.tab";
        List<String> commands = new ArrayList<String>();
        commands.add(blatBin);
        if (blatOptions != null) {
            for (String option : blatOptions) {
                commands.add(option);
            }
        }
        commands.add(databaseArg);
        commands.add(queryArg);
        commands.add(outputArg);
        commands.add("-out=blast8");
        if (blatUserOptions != null && blatUserOptions.length() > 0) {
            for (String option : blatUserOptions.split("\\s+")) {
                commands.add(option);
            }
        }
        log.println("Command:");
        for (String arg : commands) {
            log.print(arg + " ");
        }
        log.println();
        log.println();
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process p = pb.start();
        p.waitFor();
        String line;
        BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
        log.println("stdout:");
        while ((line = stdout.readLine()) != null) {
            log.println(line);
        }
        log.println();
        BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        log.println("stderr:");
        while ((line = stderr.readLine()) != null) {
            log.println(line);
        }
        log.close();
        p.destroy();
        Collection<Match> matches = new ArrayList<Match>();
        BufferedReader in = new BufferedReader(new FileReader(outputArg));
        while ((line = in.readLine()) != null) {
            matches.add(new TabDelimittedAlignment(line).convertToMatch(conf));
        }
        in.close();
        return matches;
    }

    private void deleteTmpDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        for (File f : dir.listFiles()) {
            f.delete();
        }
        dir.delete();
    }
    
    private File createTmpDir(String uniqueToken) throws SequenceSearchToolException {
        File dir = new File(tmpDir + "/" + uniqueToken + "_" + System.nanoTime());
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new SequenceSearchToolException("Error creating tmp dir: " + dir.getAbsolutePath());
            }
        }
        return dir;
    }
    
    private String createQueryFasta(File dir, String query) throws IOException {
        String queryFileName = dir.getAbsolutePath() + "/query.fa";
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(queryFileName)));
        out.println(">query");
        out.println(query);
        out.close();
        return queryFileName;
    }
    
}
