package Filer;

import java.io.*;

public class App {
    public static void main(String args[]){
        Optimizer path = new Optimizer("C:\\Users\\maximeserenfauconnie\\Downloads");
        try {
            path.getDoubles();
            System.out.println(path.trash.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
