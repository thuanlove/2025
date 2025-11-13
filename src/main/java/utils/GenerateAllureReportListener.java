package utils;

import org.testng.IExecutionListener;

import java.io.*;

public class GenerateAllureReportListener implements IExecutionListener {

    @Override
    public void onExecutionFinish() {
        try {
            System.out.println("📊 Generating Allure Report...");
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "allure generate D:\\4.HQC-Automation-Test\\vhc\\target\\allure-results --clean -o D:\\4.HQC-Automation-Test\\vhc\\target\\allure-report"
            );
            builder.inheritIO();
            builder.start().waitFor();
        } catch (Exception e) {
            System.err.println("❌ Failed to generate Allure Report:");
            e.printStackTrace();
        }
    }
}
