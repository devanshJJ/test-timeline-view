package JunitTestResult;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;


@RunListener.ThreadSafe
public class TheJunit4Listener extends RunListener {

    String TestName;
    String Test_ClassName;
    String Test_status;
    String Test_ThreadName;
    long Test_startTime;
    long Test_endTime;



    @Override
    public void testRunStarted(final Description description) {
    }

    @Override
    public void testRunFinished(final Result result) {

    }

    @Override
    public void testStarted(final Description description) {

        Test_startTime=System.currentTimeMillis();
        Test_ThreadName=ManagementFactory.getRuntimeMXBean().getName()+"."+Thread.currentThread().getName()+"("+Thread.currentThread().getId()+")";
        TestName=description.getMethodName();
        Test_ClassName=description.getClassName();
        Test_status="SUCCESS";
    }

    @Override
    public void testFinished(final Description description) {

        Test_endTime=System.currentTimeMillis();

        String json="{\"name\":\""+TestName+"\" ," +
                    " \"className\":\""+Test_ClassName+"\" ," +
                    " \"result\":\""+Test_status+"\" ," +
                    " \"StartTime\":"+Test_startTime+" ,"+
                    " \"EndTime\":"+Test_endTime+" ,"+
                    "\"Thread\":\" "+Test_ThreadName+" \" }";

        String path = "build/testResult";
        File MyReportDir = new File(path);
        if (! Files. exists(Paths. get(path))) {
            MyReportDir. mkdir();
        }

        String Json_pathName="build/testResult/"+TestName+"result.json";

        File TestResult_file = new File(Json_pathName);
        try {
            TestResult_file.createNewFile();
        } catch (IOException e) {
            System.out.println("An error occurred while creating test result json files");
            e.printStackTrace();
        }
        try {
            FileWriter TESTRESULT_Writer = new FileWriter(Json_pathName);
            TESTRESULT_Writer.write(json);
            TESTRESULT_Writer.close();
//            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing test result");
            e.printStackTrace();
        }

    }
    @Override
    public void testFailure(final Failure failure) {
        Test_status="FAILURE";
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        Test_status="SKIPPED";
    }

    @Override
    public void testIgnored(final Description description) {
        Test_status="SKIPPED";
    }


}