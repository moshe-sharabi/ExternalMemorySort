package edu.hebrew.db.external;

public class Main {
    public static void main(String[] args) {
        if (args.length != 5 && args.length != 7) {
            System.out.println("Wrong number of arguments");
            System.out.println("For part A: exercise_part "
                    + "input_file output_file column_to_sort temp_path");
            System.out.println("For parts B and C: exercise_part "
                    + "input_file output_file column_to_sort"
                    + " temp_path column_to_select substring_to_select");
            System.exit(1);
        }
        String exercisePart = args[0];
        String in = args[1];
        String out = args[2];
        int columnToSort = Integer.valueOf(args[3]);
        String tmpPath = args[4];
        IExternalMemory e = new ExternalMemoryImpl();
        if (exercisePart.equals("A") || exercisePart.equals("a")) {
            e.sort(in, out, columnToSort, tmpPath);
        } else {
            int columnToSelect = Integer.valueOf(args[5]);
            String substrToSelect = args[6];
            if (exercisePart.equals("B") || exercisePart.equals("b"))
                e.sortAndSelect(in, out, columnToSort, tmpPath, columnToSelect,
                        substrToSelect);
            else if (exercisePart.equals("C") || exercisePart.equals("c"))
                e.sortAndSelectEfficiently(in, out, columnToSort, tmpPath,
                        columnToSelect, substrToSelect);
            else
                System.out.println("Wrong usage: first argument"
                        + "should be a, b, or c only!");
        }
    }
}
