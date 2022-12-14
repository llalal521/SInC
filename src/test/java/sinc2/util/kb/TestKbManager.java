package sinc2.util.kb;

import sinc2.kb.SimpleCompressedKb;
import sinc2.util.LittleEndianIntIO;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestKbManager {
    public static final String MEM_DIR = "/dev/shm";

    protected final List<String> tempFiles = new ArrayList<>();
    protected final List<String> tempDirs = new ArrayList<>();
    protected final String kbName;
    protected final String kbPath;
    protected final String ckbName;
    protected final String ckbPath;

    public TestKbManager() throws IOException {
        /* Create a test KB in the memory disk device */
        kbName = UUID.randomUUID().toString();
        kbPath = Paths.get(MEM_DIR, kbName).toString();
        ckbName = UUID.randomUUID().toString();
        ckbPath = Paths.get(MEM_DIR, ckbName).toString();
        createTestKb();
        createTestCkb();
    }

    protected void createTestKb() throws IOException {
        File kb_dir = new File(kbPath);
        assertTrue(kb_dir.mkdir());
        createTestMapFiles();
        createTestRelationFiles();
    }

    protected void createTestMapFiles() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(NumerationMap.getMapFilePath(kbPath, 1).toFile());
        writer.println("");
        writer.println("");
        writer.println("");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(kbPath, 2).toFile());
        writer.println("alice");
        writer.println("bob");
        writer.println("catherine");
        writer.println("diana");
        writer.println("erick");
        writer.println("frederick");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(kbPath, 3).toFile());
        writer.println("gabby");
        writer.println("harry");
        writer.println("isaac");
        writer.println("jena");
        writer.println("kyle");
        writer.println("lily");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(kbPath, 4).toFile());
        writer.println("marvin");
        writer.println("nataly");
        writer.close();
    }

    protected void createTestRelationFiles() throws IOException {
        FileOutputStream fos = new FileOutputStream(KbRelation.getRelFilePath(kbPath, "family", 3, 4).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.write(LittleEndianIntIO.leInt2ByteArray(5));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.write(LittleEndianIntIO.leInt2ByteArray(7));
        fos.write(LittleEndianIntIO.leInt2ByteArray(8));
        fos.write(LittleEndianIntIO.leInt2ByteArray(9));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xa));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xb));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xc));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xd));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xe));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xf));
        fos.close();

        fos = new FileOutputStream(KbRelation.getRelFilePath(kbPath, "mother", 2, 4).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.write(LittleEndianIntIO.leInt2ByteArray(7));
        fos.write(LittleEndianIntIO.leInt2ByteArray(9));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xa));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xc));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xd));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xf));
        fos.close();

        fos = new FileOutputStream(KbRelation.getRelFilePath(kbPath, "father", 2, 4).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(5));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.write(LittleEndianIntIO.leInt2ByteArray(8));
        fos.write(LittleEndianIntIO.leInt2ByteArray(9));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xb));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xc));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0x10));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0x11));
        fos.close();
    }

    protected void createTestCkb() throws IOException {
        File ckb_dir = new File(ckbPath);
        assertTrue(ckb_dir.mkdir());
        createCkbMapFiles();
        createCkbNecessaryRelationFiles();
        createHypothesisFile();
        createCounterexampleFiles();
    }

    protected void createCkbMapFiles() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(NumerationMap.getMapFilePath(ckbPath, 1).toFile());
        writer.println("");
        writer.println("");
        writer.println("");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(ckbPath, 2).toFile());
        writer.println("alice");
        writer.println("bob");
        writer.println("catherine");
        writer.println("diana");
        writer.println("erick");
        writer.println("frederick");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(ckbPath, 3).toFile());
        writer.println("gabby");
        writer.println("harry");
        writer.println("isaac");
        writer.println("jena");
        writer.println("kyle");
        writer.println("lily");
        writer.close();

        writer = new PrintWriter(NumerationMap.getMapFilePath(ckbPath, 4).toFile());
        writer.println("marvin");
        writer.println("nataly");
        writer.close();
    }

    protected void createCkbNecessaryRelationFiles() throws IOException {
        FileOutputStream fos = new FileOutputStream(KbRelation.getRelFilePath(ckbPath, "family", 3, 3).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(7));
        fos.write(LittleEndianIntIO.leInt2ByteArray(8));
        fos.write(LittleEndianIntIO.leInt2ByteArray(9));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xa));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xb));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xc));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xd));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xe));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xf));
        fos.close();
    }

    protected void createHypothesisFile() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(Paths.get(ckbPath, SimpleCompressedKb.HYPOTHESIS_FILE_NAME).toFile());
        writer.println("father(X0,X1):-family(?,X0,X1)");
        writer.println("mother(X0,catherine):-family(X0,?,catherine)");
        writer.close();
    }

    protected void createCounterexampleFiles() throws IOException {
        FileOutputStream fos = new FileOutputStream(SimpleCompressedKb.getCounterexampleFilePath(
                ckbPath, "mother", 2, 1
        ).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(5));
        fos.write(LittleEndianIntIO.leInt2ByteArray(5));
        fos.close();

        fos = new FileOutputStream(SimpleCompressedKb.getCounterexampleFilePath(
                ckbPath, "father", 2, 2
        ).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(0x10));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0x11));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xe));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xf));
        fos.close();
    }

    protected void removeDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (null != files) {
            for (File f : files) {
                if (f.isDirectory()) {
                    removeDir(f.getAbsolutePath().toString());
                } else {
                    assertTrue(f.delete());
                }
            }
            assertTrue(dir.delete());
        }
    }

    public void appendTmpFile(String tmpFile) {
        tempFiles.add(tmpFile);
    }

    public void appendTmpDir(String dir) {
        tempDirs.add(dir);
    }

    public String createTmpDir() {
        String tmp_dir = UUID.randomUUID().toString();
        Path tmp_dir_path = Paths.get(TestKbManager.MEM_DIR, tmp_dir);
        assertTrue(tmp_dir_path.toFile().mkdir());
        tempDirs.add(tmp_dir_path.toString());
        return tmp_dir_path.toString();
    }

    public void cleanUpKb() {
        removeDir(kbPath);
        removeDir(ckbPath);
        for (String dir_path: tempDirs) {
            removeDir(dir_path);
        }
        for (String file_path: tempFiles) {
            assertTrue(new File(file_path).delete());
        }
    }

    public String getKbName() {
        return kbName;
    }

    public String getKbPath() {
        return kbPath;
    }

    public String getCkbName() {
        return ckbName;
    }

    public String getCkbPath() {
        return ckbPath;
    }
}
