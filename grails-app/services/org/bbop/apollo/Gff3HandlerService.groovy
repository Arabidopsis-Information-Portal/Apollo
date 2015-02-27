package org.bbop.apollo;

//import *;
//import util.BioObjectUtil;
//import *;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import grails.compiler.GrailsCompileStatic

//import groovy.transform.CompileStatic
//
//
//@CompileStatic
//@GrailsCompileStatic
public class Gff3HandlerService {

//    private File file;
//    private boolean firstEntry;
//    private PrintWriter out;
//    private Mode mode;
//    private Set<String> attributesToExport;

    SequenceService sequenceService
    FeatureRelationshipService featureRelationshipService
    TranscriptService transcriptService
    ExonService exonService
    FeatureService featureService
    FeaturePropertyService featurePropertyService

//    public GFF3HandlerService(String path, Mode mode, Set<String> attributesToExport) throws IOException {
//        this(path, mode, Format.TEXT, attributesToExport);
//    }
//    
//    public GFF3HandlerService(String path, Mode mode, Format format, Set<String> attributesToExport) throws IOException {
//        this.mode = mode;
//        this.attributesToExport = attributesToExport;
//        file = new File(path);
//        file.createNewFile();
//        firstEntry = true;
//        if (mode == Mode.READ) {
//            if (!file.canRead()) {
//                throw new IOException("Cannot read GFF3 file: " + file.getAbsolutePath());
//            }
//        }
//        if (mode == Mode.WRITE) {
//            if (!file.canWrite()) {
//                throw new IOException("Cannot write GFF3 to: " + file.getAbsolutePath());
//            }
//            switch (format) {
//            case Format.TEXT:
//                out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
//                break;
//            case Format.GZIP:
//                out = new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
//            }
//        }
//    }

//    public void close() {
//        if (mode == Mode.READ) {
//            //TODO
//        }
//        else if (mode == Mode.WRITE) { 
//            out.close();
//        }
//    }
    
    // adding a default attribute list to export when defaultAttributesToExport is null
    private List<String> defaultAttributesExport = ["name"]
    
    public void writeFeaturesToText(String path,Collection<? extends Feature> features, String source) throws IOException {
        WriteObject writeObject = new WriteObject( )

        writeObject.mode= Mode.WRITE
        writeObject.file= new File(path)
        writeObject.format= Format.TEXT
        if(!writeObject.attributesToExport){
            writeObject.attributesToExport = defaultAttributesExport;
        }
        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }

        println "===> Can I write to ${writeObject.file}?: ${writeObject.file.canWrite()}"
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file)));
        writeObject.out = out
        out.println("##gff-version 3");
       
        writeFeatures(writeObject,features,source)
        
    }


    public void writeFeatures(WriteObject writeObject,Collection<? extends Feature> features, String source) throws IOException {
        Map<Sequence, Collection<Feature>> featuresBySource = new HashMap<Sequence, Collection<Feature>>();
        for (Feature feature : features) {
            Sequence sourceFeature = feature.getFeatureLocation().sequence
            Collection<Feature> featureList = featuresBySource.get(sourceFeature);
            if (featureList == null) {
                featureList = new ArrayList<Feature>();
                featuresBySource.put(sourceFeature, featureList);
            }
            featureList.add(feature);
        }
        for (Map.Entry<Sequence, Collection<Feature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(writeObject,entry.getKey());
            for (Feature feature : entry.getValue()) {
                writeFeature(writeObject,feature, source);
                writeFeatureGroupEnd(writeObject.out);
            }
        }
    }

    public void writeFeatures(WriteObject writeObject,Iterator<? extends Feature> iterator, String source, boolean needDirectives) throws IOException {
//        if (firstEntry) {
//            out.println("##gff-version 3");
//            firstEntry = false;
//        }
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            if (needDirectives) {
                writeGroupDirectives(writeObject,feature.getFeatureLocation().sequence)
                needDirectives = false;
            }
            writeFeature(writeObject,feature, source);
            writeFeatureGroupEnd(writeObject.out);
        }
    }

    static private void writeGroupDirectives(WriteObject writeObject,Sequence sourceFeature) {
        if (sourceFeature.getFeatureLocations().size() == 0) return;
        FeatureLocation loc = sourceFeature.getFeatureLocations().iterator().next();
        writeObject.out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, loc.getFmin() + 1, loc.getFmax()));
    }

    static private void writeFeatureGroupEnd(PrintWriter out) {
        out.println("###");
    }

    static private void writeEmptyFastaDirective(PrintWriter out) {
        out.println("##FASTA");
    }

    private void writeFeature(WriteObject writeObject,Feature feature, String source) {
        for (GFF3Entry entry : convertToEntry(writeObject,feature, source)) {
            writeObject.out.println(entry.toString());
        }
    }

    public void writeFasta(PrintWriter out,Collection<? extends Feature> features) {
        writeEmptyFastaDirective(out);
        for (Feature feature : features) {
            writeFasta(out,feature, false);
        }
    }

    public void writeFasta(PrintWriter out,Feature feature) {
        writeFasta(out,feature, true);
    }

    public void writeFasta(PrintWriter out,Feature feature, boolean writeFastaDirective) {
        writeFasta(out,feature, writeFastaDirective, true);
    }

    public void writeFasta(PrintWriter out,Feature feature, boolean writeFastaDirective, boolean useLocation) {
        int lineLength = 60;
        if (writeFastaDirective) {
            writeEmptyFastaDirective(out);
        }
        String residues = null;
        if (useLocation) {
            FeatureLocation loc = feature.getFeatureLocations().iterator().next();
            residues = sequenceService.getResidueFromFeatureLocation(loc)
        } else {
            residues = sequenceService.getResiduesFromFeature(feature)
        }
        if (residues != null) {
            out.println(">" + feature.getUniqueName());
            int idx = 0;
            while (idx < residues.length()) {
                out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())));
                idx += lineLength;
            }
        }
    }

    private Collection<GFF3Entry> convertToEntry(WriteObject writeObject,Feature feature, String source) {
        List<GFF3Entry> gffEntries = new ArrayList<GFF3Entry>();
        convertToEntry(writeObject,feature, source, gffEntries);
        return gffEntries;
    }

    private void convertToEntry(WriteObject writeObject,Feature feature, String source, Collection<GFF3Entry> gffEntries) {
        String[] cvterm = feature.cvTerm.split(":");
        String seqId = feature.getFeatureLocation().sequence.name
        //String type = cvterm[1];
        String type = feature.cvTerm;
        println "===> type in convertToEntry: ${type}"
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
        String score = "";
        String strand;
        if (feature.getStrand() == 1) {
            strand = "+";
        } else if (feature.getStrand() == -1) {
            strand = "-";
        } else {
            strand = "";
        }
        String phase = "";
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
        entry.setAttributes(extractAttributes(writeObject,feature));
        gffEntries.add(entry);
        println "===> does feature.getChildFeatureRelationships() look like a null: ${feature.getChildFeatureRelationships()}"
        if (feature.getChildFeatureRelationships() != null) {
            for (Feature child : featureRelationshipService.getChildren(feature)) {
                if (child instanceof CDS) {
                    convertToEntry(writeObject, (CDS) child, source, gffEntries);
                } else {
                    convertToEntry(writeObject, child, source, gffEntries);
                }
            }
        }
        else {
            println "===> WARNING: ${feature} has null ChildFeatureRelationships"
            
        }
        println "===> gff3Entries: ${gffEntries.toString()}"
    }

    private void convertToEntry(WriteObject writeObject,CDS cds, String source, Collection<GFF3Entry> gffEntries) {
//        String []cvterm = cds.getType().split(":");
        String seqId = cds.getFeatureLocation().sequence.name
//        String type = cvterm[1];
        String type = cds.cvTerm
        String score = "";
        String strand;
        if (cds.getStrand() == 1) {
            strand = "+";
        } else if (cds.getStrand() == -1) {
            strand = "-";
        } else {
            strand = "";
        }
//        featureRelationshipService.getParentForFeature(cds,transcriptService.ontologyIds)


        Transcript transcript = transcriptService.getParentTranscriptForFeature(cds)

        List<Exon> exons = exonService.getSortedExons(transcript)
//        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(cds.getTranscript().getExons());
        int length = 0;
        for (Exon exon : exons) {
            if (!featureService.overlaps(exon, cds)) {
                continue;
            }
            int fmin = exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin();
            int fmax = exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax();
            String phase;
            if (length % 3 == 0) {
                phase = "0";
            } else if (length % 3 == 1) {
                phase = "2";
            } else {
                phase = "1";
            }
            length += fmax - fmin;
            GFF3Entry entry = new GFF3Entry(seqId, source, type, fmin + 1, fmax, score, strand, phase);
            entry.setAttributes(extractAttributes(writeObject,cds));
            gffEntries.add(entry);
        }
        for (Feature child : featureRelationshipService.getChildren(cds)) {
            convertToEntry(writeObject,child, source, gffEntries);
        }
    }

    private Map<String, String> extractAttributes(WriteObject writeObject, Feature feature) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(feature.getUniqueName()));
        println "===> feature.getName() in extractAttributes(): ${feature.getName()}"
        if (feature.getName() != null && writeObject.attributesToExport.contains(FeatureStringEnum.NAME.value)) {
            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(feature.getName()));
            println "===> attributes map for feature: ${attributes.toString()}"
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.SYNONYMS.value)) {
            Iterator<Synonym> synonymIter = feature.synonyms.iterator();
            if (synonymIter.hasNext()) {
                StringBuilder synonyms = new StringBuilder();
                synonyms.append(synonymIter.next().getName());
                while (synonymIter.hasNext()) {
                    synonyms.append(",");
                    synonyms.append(encodeString(synonymIter.next().getName()));
                }
                attributes.put(FeatureStringEnum.EXPORT_ALIAS.value, synonyms.toString());
            }
        }

        // gets the top writeable object
//        SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
//        Feature simpleFeature = (Feature)iterator.next();
//        Iterator<FeatureRelationship> frIter = simpleFeature.getParentFeatureRelationships().iterator();
//        if (frIter.hasNext()) {
//            StringBuilder parents = new StringBuilder();
//            parents.append(encodeString(frIter.next().getObjectFeature().getUniqueName()));
//            while (frIter.hasNext()) {
//                parents.append(",");
//                parents.append(encodeString(frIter.next().getObjectFeature().getUniqueName()));
//            }
//            attributes.put("Parent", parents.toString());
//        }
        println "===> does feature.getParentFeatureRelationships() look like a null: ${feature.getParentFeatureRelationships()}"
        if (feature.getParentFeatureRelationships() != null) {

            Iterator<FeatureRelationship> frIter = featureRelationshipService.getParentForFeature(feature).iterator();
            if (frIter.hasNext()) {
                StringBuilder parents = new StringBuilder();
                parents.append(encodeString(frIter.next().parentFeature.uniqueName));
                while (frIter.hasNext()) {
                    parents.append(",");
                    parents.append(encodeString(frIter.next().parentFeature.uniqueName));
                }
                attributes.put(FeatureStringEnum.EXPORT_PARENT.value, parents.toString());
            }
        }
        else {
            println "===> WARNING: ${feature} has null ParentFeatureRelationships"
        }
        //TODO: Target
        //TODO: Gap
        if (writeObject.attributesToExport.contains(FeatureStringEnum.COMMENTS.value)) {
            Iterator<Comment> commentIter = featurePropertyService.getComments(feature).iterator()
            if (commentIter.hasNext()) {
                StringBuilder comments = new StringBuilder();
                comments.append(encodeString(commentIter.next().value));
                while (commentIter.hasNext()) {
                    comments.append(",");
                    comments.append(encodeString(commentIter.next().value));
                }
                attributes.put(FeatureStringEnum.EXPORT_NOTE.value, comments.toString());
            }
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DBXREFS.value)) {
            Iterator<DBXref> dbxrefIter = feature.featureDBXrefs.iterator();
            if (dbxrefIter.hasNext()) {
                StringBuilder dbxrefs = new StringBuilder();
                DBXref dbxref = dbxrefIter.next();
                dbxrefs.append(encodeString(dbxref.getDb().getName() + ":" + dbxref.getAccession()));
                while (dbxrefIter.hasNext()) {
                    dbxrefs.append(",");
                    dbxref = dbxrefIter.next();
                    dbxrefs.append(encodeString(dbxref.getDb().getName()) + ":" + encodeString(dbxref.getAccession()));
                }
                attributes.put(FeatureStringEnum.EXPORT_DBXREF.value, dbxrefs.toString());
            }
        }
        if (feature.getDescription() != null && writeObject.attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value)) {
            attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(feature.getDescription()));
        }
        if (feature.getStatus() != null && writeObject.attributesToExport.contains(FeatureStringEnum.STATUS.value)) {
            attributes.put(FeatureStringEnum.STATUS.value, encodeString(feature.getStatus().value));
        }
        if (feature.getSymbol() != null && writeObject.attributesToExport.contains(FeatureStringEnum.SYMBOL.value)) {
            attributes.put(FeatureStringEnum.SYMBOL.value, encodeString(feature.getSymbol()));
        }
        //TODO: Ontology_term
        //TODO: Is_circular
        Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
        if (writeObject.attributesToExport.contains(FeatureStringEnum.ATTRIBUTES.value)) {
            if (propertyIter.hasNext()) {
                Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                while (propertyIter.hasNext()) {
                    FeatureProperty prop = propertyIter.next();
                    StringBuilder props = properties.get(prop.getTag());
                    if (props == null) {
                        props = new StringBuilder();
                        properties.put(prop.getTag(), props);
                    } else {
                        props.append(",");
                    }
                    props.append(encodeString(prop.getValue()));
                }
                for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                    attributes.put(encodeString(iter.getKey()), iter.getValue().toString());
                }
            }
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.OWNER.value)) {
            attributes.put(FeatureStringEnum.OWNER.value, encodeString(feature.getOwner().username));
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.dateCreated);
            attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.lastUpdated);
            attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        return attributes;
    }

    static private String encodeString(String str) {
        return str.replaceAll(",", "%2C").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09");
    }

    public class GFF3Entry {

        private String seqId;
        private String source;
        private String type;
        private int start;
        private int end;
        private String score;
        private String strand;
        private String phase;
        private Map<String, String> attributes;

        public GFF3Entry(String seqId, String source, String type, int start, int end, String score, String strand, String phase) {
            this.seqId = seqId;
            this.source = source;
            this.type = type;
            this.start = start;
            this.end = end;
            this.score = score;
            this.strand = strand;
            this.phase = phase;
            this.attributes = new HashMap<String, String>();
        }

        public String getSeqId() {
            return seqId;
        }

        public void setSeqId(String seqId) {
            this.seqId = seqId;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getStrand() {
            return strand;
        }

        public void setStrand(String strand) {
            this.strand = strand;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public void addAttribute(String key, String value) {
            attributes.put(key, value);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("%s\t%s\t%s\t%d\t%d\t%s\t%s\t%s\t", getSeqId(), getSource(), getType(), getStart(), getEnd(), getScore(), getStrand(), getPhase()));
            Iterator<Map.Entry<String, String>> iter = attributes.entrySet().iterator();
            if (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                buf.append(entry.getKey());
                buf.append("=");
                buf.append(entry.getValue());
                while (iter.hasNext()) {
                    entry = iter.next();
                    buf.append(";");
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue());
                }
            }
            return buf.toString();
        }
    }

    public enum Mode {
        READ,
        WRITE
    }

    public enum Format {
        TEXT,
        GZIP
    }


    private class WriteObject {
        File file;
//        boolean firstEntry;
        PrintWriter out;
        Mode mode;
        Set<String> attributesToExport;
        Format format
    }

}
