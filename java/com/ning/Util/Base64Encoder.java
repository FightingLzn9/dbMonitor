package com.ning.Util;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;

public class Base64Encoder {

        public String Base64ing(String filepath){
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(filepath));
                return Base64.getEncoder().encodeToString(fileContent);
            } catch (FileNotFoundException e) {
                System.err.println("文件 '" + filepath + "' 找不到。");
            } catch (IOException e) {
                System.err.println("发生错误: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("发生未知错误: " + e.getMessage());
            }
            return null;
        }
}