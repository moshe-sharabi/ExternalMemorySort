package edu.hebrew.db.external;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
public interface IExternalMemory {
    public void sort(String in, String out, int colNumSort, String tmpPath);
    public void select(String in, String out, int colNumSelect, String substrSelect,
                       String tmpPath);
    public void sortAndSelectEfficiently(String in, String out, int colNumSort,
                                         String tmpPath, int colNumSelect, String substrSelect);
    /**
     * Do not modify this method!
     *
     * The method sorts the input file in a lexicographic order of a column, selects
     * rows according to the selection condition (colNumSelect and substrSelect), and
     * saves the result in an output file.
     *
     * @param in
     * @param out
     * @param colNumSort
     * @param tmpPath
     * @param colNumSelect
     * @param substrSelect
     */
    default public void sortAndSelect(String in, String out, int colNumSort,
                                      String tmpPath, int colNumSelect, String substrSelect) {
        String outFileName = new File(out).getName();
        String tmpFileName = outFileName.substring(0, outFileName.lastIndexOf('.'))
                + "_intermed" + outFileName.substring(outFileName.lastIndexOf('.'));
        String tmpOut = Paths.get(tmpPath, tmpFileName).toString();
        this.sort(in, tmpOut, colNumSort, tmpPath);
        this.select(tmpOut, out, colNumSelect, substrSelect, tmpPath);
        try {
            Files.deleteIfExists(Paths.get(tmpPath, tmpFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}