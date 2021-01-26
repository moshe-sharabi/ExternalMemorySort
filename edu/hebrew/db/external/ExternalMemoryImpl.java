package edu.hebrew.db.external;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ExternalMemoryImpl implements IExternalMemory {

    private static final int bytesInBlock = 20000; // Y
    private static final int blocksInRam = 1000; // M
    private int columnCompare;
    private int columnSelect;

    class LinesComparator implements Comparator<String> {
        //Comparator class for two string lines by the column chosen by the user
        @Override
        public int compare(String o1, String o2) {
            String[] s1 = o1.split(" ");
            String[] s2 = o2.split(" ");
            return s1[columnCompare].compareTo(s2[columnCompare]);
        }
    }

    private String fixPath(String path) {
        // ensure that directory path end with slash (changes by os)
        if (path.endsWith(File.separator)) return path;
        return path + File.separator;
    }

    private int getBytesOfLine(String in) throws FileNotFoundException {
        //returns bytes of ONE line in the table file
        File file = new File(in);
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            String st = br.readLine();
            return st.length() * 2;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private LinkedList<File> splitToSortedFiles(String in, String tmpPath, boolean select, String subStr) throws IOException {
        // split the origin big one to small sorted ones. the splatted files size is blocksInRam * bytesInBlock
        int sizeOfLine = getBytesOfLine(in); // X
        int linesInRam = blocksInRam * (bytesInBlock / sizeOfLine);
        LinkedList<File> filesList = new LinkedList<>();

        File file = new File(in);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st = "";
        int filesCounter = 0;
        while (st != null) { //loop over lines in the table
            LinkedList<String> lines = new LinkedList<>();

            for (int i = 0; i < linesInRam; i++) {
                st = br.readLine();
                if ((st == null)) break;
                if (!select) {
                    lines.add(st);
                    continue;
                }
                if (subSelect(st, subStr, columnSelect)) {
                    lines.add(st);
                    continue;
                }
                i--;
            }

            File statText = new File(tmpPath + "sorted" + filesCounter);
            filesList.add(statText);
            filesCounter++;
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            lines.sort(new LinesComparator());
            for (String line : lines) w.write(line + "\n");
            w.close();
        }
        return filesList;
    }


    private File merge_small_runs(List<File> runsToMerge, String newRunName) {
        // this function sort and merge list of runs into new run and returns it
        File newRun = new File(newRunName);
        ArrayList<BufferedReader> bufferReaders = null;
        BufferedWriter writeNewRun = null;
        try {
            //first initialize the buffers
            bufferReaders = new ArrayList<>();

            for (File curRun : runsToMerge) {
                FileReader fileReader = new FileReader(curRun);
                bufferReaders.add(new BufferedReader(fileReader));
            }
            FileWriter fileWriter = new FileWriter(newRun);
            writeNewRun = new BufferedWriter(fileWriter);

            //now write the new merged run
            ArrayList<String> lines = new ArrayList<>();
            for (BufferedReader reader : bufferReaders) {
                lines.add(reader.readLine());
            }
            while (!lines.isEmpty()) {
                int minIndex = 0;
                String minValue = lines.get(minIndex);
                for (int i = 1; i < lines.size(); ++i) {
                    String[] s1 = minValue.split(" ");
                    if (lines.get(i) == null) {
                        continue;
                    }
                    String[] s2 = (lines.get(i)).split(" ");

                    if (s1[columnCompare].compareTo(s2[columnCompare]) > 0) {
                        minIndex = i;
                        minValue = lines.get(i);
                    }
                }

                //now replace the min value line with the next line in bufferedReader
                // and write the min value line to the new run!!
                String newLine = bufferReaders.get(minIndex).readLine();
                if (newLine == null) {
                    lines.remove(minIndex);
                    bufferReaders.remove(minIndex);
                } else {
                    lines.set(minIndex, newLine);
                }
                if (minValue != null) {
                    writeNewRun.write(minValue);
                }

                if (!lines.isEmpty()) {
                    writeNewRun.newLine();
                }
            }
            writeNewRun.flush();


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferReaders != null) {
                    for (BufferedReader reader : bufferReaders) {
                        reader.close();
                    }
                }
                if (writeNewRun != null) {
                    writeNewRun.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return newRun;

    }

    private File mergeRuns(List<File> sortedFiles, String tmpPath) {
        //this function merge some runs (from sortedFiles) into new run on a new file
        List<File> saveOldRunsBeforeMerge = new LinkedList<>();
        int counter1 = 0;
		//int numBuffer = blocksInRam -100; //save one block to output and 99 blocks for java variables etc.
        int numBuffer = 20; //save one block to output and 99 blocks for java variables etc.

        while (sortedFiles.size() > 1) {
            int curIndex = 0;
            int counter2 = 0;
            List<File> newRuns = new ArrayList<>();
            while (curIndex < sortedFiles.size()) {
                String newRunName = tmpPath + "run_File_" + counter1 + "_" + counter2;
                int index = Math.min(curIndex + numBuffer, sortedFiles.size());
                List<File> runsToMerge = sortedFiles.subList(curIndex, index);
                File newRun = merge_small_runs(runsToMerge, newRunName);
                newRuns.add(newRun);

                curIndex = curIndex + numBuffer;
                counter2++;
            }
            saveOldRunsBeforeMerge.addAll(sortedFiles);
            sortedFiles = newRuns;//next iteration - merge the new runs created
            counter1++;

        }
        //now delete the old files/runs which created
        while (!saveOldRunsBeforeMerge.isEmpty()) {
            File toDelete = saveOldRunsBeforeMerge.remove(0);
            if (!toDelete.delete()) {
                System.err.println("cant delete this file: " + toDelete.getName());
            }
        }
        return sortedFiles.get(0);

    }


    private boolean subSelect(String line, String subString, int col) {
        String[] trimmedLine = line.split(" ");
        return trimmedLine[col - 1].contains(subString);
    }

    private void writeToFile(List<String> lines, Writer w) throws IOException {
        for (String line : lines) w.write(line + "\n");
    }


    @Override
    public void sort(String in, String out, int colNum, String tmpPath) {
        columnCompare = colNum - 1;
        tmpPath = fixPath(tmpPath);

        try {
            LinkedList<File> sortedFiles = splitToSortedFiles(in, tmpPath, false, null);
            File last_merged_run = mergeRuns(sortedFiles, tmpPath);
            //rename the last run to be returned as the output file
            File outFile = new File(out);

            last_merged_run.renameTo(outFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void select(String in, String out, int colNumSelect,
                       String subStrSelect, String tmpPath) {

        try {
            int sizeOfLine = getBytesOfLine(in); // X
            int linesInRam = blocksInRam * (bytesInBlock / sizeOfLine);

            File file = new File(in);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            int linesCounter = 0;


            File statText = new File(out);
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);

            LinkedList<String> lines = new LinkedList<>();

            while ((st = br.readLine()) != null) {
                if (subSelect(st, subStrSelect, colNumSelect)) {
                    lines.add(st);
                    if (linesCounter++ > linesInRam) {
                        linesCounter = 0;
                        writeToFile(lines, w);
                        lines.clear();
                    }
                }
            }
            writeToFile(lines, w);
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void sortAndSelectEfficiently(String in, String out, int colNumSort,
                                         String tmpPath, int colNumSelect, String substrSelect) {
        columnCompare = colNumSort - 1;
        columnSelect = colNumSelect;
        tmpPath = fixPath(tmpPath);

        try {
            LinkedList<File> sortedFiles = splitToSortedFiles(in, tmpPath, true, substrSelect);
            File last_merged_run = mergeRuns(sortedFiles, tmpPath);
            //rename the last run to be returned as the output file
            File outFile = new File(out);

            last_merged_run.renameTo(outFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}