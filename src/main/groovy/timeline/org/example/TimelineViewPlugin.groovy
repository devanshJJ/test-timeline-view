package timeline.org.example

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.text.DateFormat
import java.text.SimpleDateFormat

public class TimelineViewPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {


        // configuration for adding RunListener
        project.tasks.named('test'){
            jvmArgs "-javaagent:${classpath.find { it.name.contains('aspectjweaver') }.absolutePath}"

            doFirst (){
//                println "deleting the previous results"
                File testreultFolder = new File("build/test-results/individual-test-results")
                testreultFolder.deleteDir() ;

            }
            finalizedBy project.tasks.named('createTimelineViewReport')

        }

        project.dependencies {

            testImplementation 'org.aspectj:aspectjweaver:1.9.9.1'
            testImplementation 'org.aspectj:aspectjrt:1.9.9.1'
            testImplementation 'io.timeline.junitTest.result:junit-test-result-accumulator:1.0.1'
        }



        project.tasks.create('createTimelineViewReport',TimelineViewTask)



    }
}


class TimelineViewTask extends DefaultTask{

    @TaskAction
    def createTimelineViewReport () {

        long hourInMs = 60 * 60 * 1000;
        long timeInMin = 60 * 1000;
        File reportfol = new File("build/reports/timeline")


        reportfol.mkdir()
        File report = new File("build/reports/timeline/Report.html")
        if (report.exists()) {
            assert report.delete()
            assert report.createNewFile()
        }

        //reading test results and storing in map
        def JSON_files =  project.file("${project.rootDir}/build/test-results/individual-test-results").listFiles()
        int noOfTest=JSON_files.size()
        def mapClassToTestName = [:]
        def mapOwnerToTestName = [:]
        def mapTestNameToTestsRunResult  = [:]
        def mapThreadNameToTestsRunResult  = [:]
        HashSet<String> set=new HashSet<String>()
        long executionStarted =System.currentTimeMillis()
        long executionEnded =0

        def minHeap = new PriorityQueue<Long>()

        JSON_files.each { File file ->
            if (file.isFile()) {
                Scanner myScanner = new Scanner(file);

                String result_JSON = myScanner.nextLine();
                def parsedJson_result = new groovy.json.JsonSlurper().parseText(result_JSON)
                set.add(parsedJson_result.Thread)
                def list = []
                executionStarted=Math.min(executionStarted,parsedJson_result.StartTime)
                executionEnded=Math.max(executionEnded,parsedJson_result.EndTime)
                if(minHeap.size()*10>=noOfTest){
                    long tempTime=parsedJson_result.EndTime-parsedJson_result.StartTime
                    if(tempTime>minHeap.peek()){
                        minHeap.poll()
                        minHeap.add(tempTime)
                    }
                }
                else
                minHeap.add(parsedJson_result.EndTime-parsedJson_result.StartTime)
                //mapping test with className
                String mapKey = parsedJson_result.name + "#" + parsedJson_result.className;
                if (mapClassToTestName.containsKey(parsedJson_result.className)) {
                    list = mapClassToTestName[parsedJson_result.className]
                }

                list.add(mapKey)
                mapClassToTestName[parsedJson_result.className] = list

                list=[]
                if (mapThreadNameToTestsRunResult.containsKey(parsedJson_result.Thread)) {
                    list = mapThreadNameToTestsRunResult[parsedJson_result.Thread]
                }

                list.add(mapKey)
                mapThreadNameToTestsRunResult[parsedJson_result.Thread] = list



                //if testOwner is specified, then map owner with test name
                if (parsedJson_result.testOwner.length() > 0) {

                    list = []
                    if (mapOwnerToTestName.containsKey(parsedJson_result.testOwner)) {
                        list = mapOwnerToTestName[parsedJson_result.testOwner]
                    }
                    list.add(mapKey)
                    mapOwnerToTestName[parsedJson_result.testOwner] = list
                }


                //mapping test result with test name
                list = []
                if (mapTestNameToTestsRunResult.containsKey(mapKey)) {
                    list = mapTestNameToTestsRunResult[mapKey]
                }
                list.add(parsedJson_result)
                mapTestNameToTestsRunResult[mapKey] = list


            }
        }


        //----------------writing HTML file
        boolean append = true
        FileWriter fileWriter = new FileWriter(report, append)
        BufferedWriter buffWriter = new BufferedWriter(fileWriter)

        buffWriter.write("<html>\n")
        buffWriter.write("<head>\n")

        //------------------some inline CSS
        buffWriter.write(" <style type=\"text/css\">\n" +
                "        #r1:checked ~  #timeline-container{\n" +
                "        display:inline-block !important;}\n" +
                "\n" +
                "         #r2:checked ~ #report-container {\n" +
                "         display:block !important;}\n" +
                "         label:hover{\n" +
                "         font-weight:bold;\n" +
                "\n" +
                "         }" +
                "         #r3:checked ~ #Test-Ownership-container {\n" +
                        "         display:block !important;}\n" +
                "        body{margin:0;}\n"+
                "        #radio-timeline:checked ~  #timeline{\n" +
                "        display:block !important;}\n" +
                "        #radio-timeline-highlight:checked ~  #timeline-highlighted{\n" +
                "        visibility:visible !important;}"+
                "        </style>\n");



        //JS
        buffWriter.write("     <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n")
        buffWriter.write("     <script type=\"text/javascript\">\n")
        buffWriter.write("      google.charts.load('current', {'packages':['timeline']});\n")
        buffWriter.write("      google.charts.setOnLoadCallback(drawChart);\n")
        buffWriter.write("      google.charts.setOnLoadCallback(drawHighlightedChart);\n")
        buffWriter.write("      function drawChart() {\n")
        buffWriter.write("        var timelineContainer = document.getElementById('timeline');\n")
        buffWriter.write("        var chart = new google.visualization.Timeline(timelineContainer);\n")
        buffWriter.write("        var dataTable = new google.visualization.DataTable();\n")
        buffWriter.write("        dataTable.addColumn({ type: 'string', id: 'Thread' });\n")
        buffWriter.write("        dataTable.addColumn({ type: 'string', id: 'Name' });\n")
        buffWriter.write("        dataTable.addColumn({ type: 'string', id: 'style', role: 'style' });\n")
        buffWriter.write("        dataTable.addColumn({ type: 'date', id: 'Start' });\n")
        buffWriter.write("        dataTable.addColumn({ type: 'date', id: 'End' });\n")
        buffWriter.write("    dataTable.addRows([\n")


        DateFormat format = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
        format.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));

        // giving result to google timeline chart to draw chart
        mapThreadNameToTestsRunResult.each{entry ->
            def list=entry.value
            list.unique(false).each {
                mapTestNameToTestsRunResult[it].sort{a, b -> a.StartTime-b.StartTime}
                mapTestNameToTestsRunResult[it] .each {parsedJson_result->


                        Date dateStart = new Date(parsedJson_result.StartTime-executionStarted);
                        String startTime = format.format(dateStart);
                        Date dateEnd = new Date(parsedJson_result.EndTime-executionStarted);
                         String endTime = format.format(dateEnd);

                        // making date format work with google chart
                        if (endTime.charAt(20) == '0') {
                        endTime = endTime.substring(0, 20) + endTime.substring(21)
                        }
                         if (startTime.charAt(20) == '0') {
                        startTime = startTime.substring(0, 20) + startTime.substring(21);
                        }

                         if (parsedJson_result.result == "SUCCESS"){
                             buffWriter.write("      [ '${parsedJson_result.Thread}', '${parsedJson_result.name}','#93e078', new Date(${startTime}), new Date(${endTime}) ],\n")
                         }
                         else {
                            buffWriter.write("      [ '${parsedJson_result.Thread}', '${parsedJson_result.name}','red', new Date(${startTime}), new Date(${endTime}) ],\n")
                         }
                }
            }
        }

        buffWriter.write("]);\n")
        buffWriter.write("    var options = {\n"+
                         "      timeline: { showRowLabels: false },alternatingRowStyle:false\n"+
                         "    };\n"+
                         "    chart.draw(dataTable,options);\n")
        buffWriter.write("  }\n")









        buffWriter.write("      function drawHighlightedChart() {\n")
        buffWriter.write("        var highlightContainer = document.getElementById('timeline-highlighted');\n")
        buffWriter.write("        var highlightedChart = new google.visualization.Timeline(highlightContainer);\n")
        buffWriter.write("    var options = {\n"+
                "      timeline: { showRowLabels: false },alternatingRowStyle:false\n"+
                "    };\n")

        // creating chart for highlighted elements
        buffWriter.write("        var dataTableHighlighted = new google.visualization.DataTable();\n")
        buffWriter.write("        dataTableHighlighted.addColumn({ type: 'string', id: 'Thread' });\n")
        buffWriter.write("        dataTableHighlighted.addColumn({ type: 'string', id: 'Name' });\n")
        buffWriter.write("        dataTableHighlighted.addColumn({ type: 'string', id: 'style', role: 'style' });\n")
        buffWriter.write("        dataTableHighlighted.addColumn({ type: 'date', id: 'Start' });\n")
        buffWriter.write("        dataTableHighlighted.addColumn({ type: 'date', id: 'End' });\n")
        buffWriter.write("    dataTableHighlighted.addRows([\n")
        mapThreadNameToTestsRunResult.each{entry ->
            def list=entry.value
            boolean atleastOneTest=false;

            list.unique(false).each {

                mapTestNameToTestsRunResult[it].sort{a, b -> a.StartTime-b.StartTime}
                mapTestNameToTestsRunResult[it] .each {parsedJson_result->

                    long tempTime=parsedJson_result.EndTime-parsedJson_result.StartTime
                    if(tempTime>=minHeap.peek()){
                        Date dateStart = new Date(parsedJson_result.StartTime-executionStarted);
                        String startTime = format.format(dateStart);
                        Date dateEnd = new Date(parsedJson_result.EndTime-executionStarted);
                        String endTime = format.format(dateEnd);
                        atleastOneTest=true;
                        // making date format work with google chart
                        if (endTime.charAt(20) == '0') {
                            endTime = endTime.substring(0, 20) + endTime.substring(21)
                        }
                        if (startTime.charAt(20) == '0') {
                            startTime = startTime.substring(0, 20) + startTime.substring(21);
                        }
                        buffWriter.write("      [ '${parsedJson_result.Thread}', '${parsedJson_result.name}','#ff6a33', new Date(${startTime}), new Date(${endTime}) ],\n")
                    }
                }
            }
            if(!atleastOneTest){
                Date dateStart = new Date(1);
                String startTime = format.format(dateStart);
                buffWriter.write("      [ '${entry.key}', '','white', new Date(${startTime}), new Date(${startTime}) ],\n")
            }
        }

        buffWriter.write("]);\n")
        buffWriter.write("    highlightedChart.draw(dataTableHighlighted,options);\n")
        buffWriter.write("  }\n")


        buffWriter.write("function report_decrease_timelinezoom () {\n" +
                " const radio1=document.getElementById('radio-timeline');\n" +
                " var currentChart='';\n" +
                " if(radio1.checked) {\n" +
                "   currentChart='timeline';\n" +
                " }\n" +
                " else{\n" +
                "   currentChart='timeline-highlighted';\n" +
                "                }"+
                " const item=document.getElementById(currentChart);\n" +
                " const noOfDigitsInHeight=item.style.width.length\n"+
                " var initial=parseInt(item.style.width.substring(0,noOfDigitsInHeight-1))-100\n" +
                " if(initial>=100){\n" +
                "           item.style.width=initial+\"%\";\n" +
                "            if(radio1.checked){\n" +
                "           drawChart();}\n" +
                "           else{\n" +
                "           drawHighlightedChart();}\n" +
                "         }\n" +
                "    }\n");
        buffWriter.write("  function report_increase_timelinezoom () {\n" +
                " const radio1=document.getElementById('radio-timeline');\n" +
                " var currentChart='';\n" +
                " if(radio1.checked) {\n" +
                "   currentChart='timeline';\n" +
                " }\n" +
                " else{\n" +
                "   currentChart='timeline-highlighted';\n" +
                "                }"+
                "       const item=document.getElementById(currentChart);\n" +
                "               const noOfDigitsInHeight=item.style.width.length\n"+
                "          var initial=parseInt(item.style.width.substring(0,noOfDigitsInHeight-1))+100\n" +
                "           item.style.width=initial+\"%\";\n" +
                "               if(radio1.checked){\n" +
                "           drawChart();}\n" +
                "           else{\n" +
                "           drawHighlightedChart();}\n" +
                "    }\n")

        buffWriter.write(" function toggleVisibility(e){\n" +
                "\n" +
                "       const item=document.getElementById(e);\n" +
                "\n" +
                "       if(item.style.display==\"none\"){\n" +
                "       item.style.display=\"inline-block\";\n" +
                "       }\n" +
                "       else\n" +
                "       item.style.display=\"none\";\n" +
                "\n" +
                "\n" +
                "\n" +
                "  }")
        buffWriter.write(" </script>\n")
        buffWriter.write("</head>\n")






        buffWriter.write("<body>\n")

        //NavBar
        buffWriter.write("<header style=\"height:5vh; width:100%; background-color: #93e078; display:flex; justify-content:space-around; align-items:center; font-size:1.7rem; border-bottom:solid 20px yellow; box-shadow: 0px 20px lightyellow;\" >\n")
        buffWriter.write("    <label for=\"r1\" style=\"cursor:pointer\">Timeline View</label>\n")
        buffWriter.write("    <label for=\"r2\" style=\"cursor:pointer\">Behaviours</label>\n")
        buffWriter.write("    <label for=\"r3\" style=\"cursor:pointer\"> Test Ownership</label>\n")
        buffWriter.write("</header>\n")
        buffWriter.write("<input type=\"radio\"  style=\"visibility:hidden\" id=\"r1\" name=\"navbar\" value=\"Timeline\" checked>\n" +
                         "<input type=\"radio\" style=\"visibility:hidden\" id=\"r2\" name=\"navbar\" value=\"Report\">"+
                         "<input type=\"radio\" style=\"visibility:hidden\" id=\"r3\" name=\"navbar\" value=\"Ownership\">")




        //Timeline
        buffWriter.write("<div id=\"timeline-container\" style=\"display:none;width: 100%; position: relative;\">\n")
        buffWriter.write("<h1> Test Timeline View</h1>\n")
        buffWriter.write("<div style=\"width: 97%; position: relative; margin:auto; \">\n")
        buffWriter.write("    <button onClick=\"report_decrease_timelinezoom()\"  style=\" position: relative;float:right; cursor:pointer; font-size:1.5rem; border: solid 2px #dbdbdb; padding:0px 7px;\">-</button>\n")
        buffWriter.write("     <button onClick=\"report_increase_timelinezoom()\"  style=\" position: relative;float:right; cursor:pointer;font-size:1.5rem; border: solid 2px #dbdbdb; padding:0px 5px;\">+</button>\n")
        buffWriter.write("<div style=\"width: 100%;height:${120+40*set.size()}px; position: relative; margin:auto; overflow-x: scroll;overflow-y:hidden;\">\n")
        buffWriter.write("<input type=\"radio\"  style=\"visibility:hidden\" id=\"radio-timeline\" name=\"timelineContainer\"checked >\n" +
                "    <input type=\"radio\" style=\"visibility:hidden\" id=\"radio-timeline-highlight\" name=\"timelineContainer\" >")
        buffWriter.write("<div id=\"timeline\" style=\"width:100%;  height:${120+40*set.size()}px;display:none;\"></div>\n")
        buffWriter.write("<div id=\"timeline-highlighted\" style=\"width:100%;  height:${120+40*set.size()}px;visibility:hidden;\"></div>\n")


        buffWriter.write("</div>\n")
        buffWriter.write("</div>\n")
        buffWriter.write("<label for=\"radio-timeline\" style=\"cursor:pointer\"><span style=\"background-color:#93e078; font-size:1rem; padding:5px; font-weight:bold; color:white;\">-</span> Timeline view of execution of all tests.</label>\n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <label for=\"radio-timeline-highlight\" style=\"cursor:pointer;\"><span style=\"background-color:#ff6a33; font-size:1rem; padding:5px; font-weight:bold; color:white;\">-</span> Timeline view of execution of test with 90 percentile in runtime.</label>\n"
             )
        buffWriter.write("</div>\n")




        //BEHAVIOR
        buffWriter.write("     <div id=\"report-container\" style=\"display:none;width: 100%; position: relative;\">\n")
        buffWriter.write("<h1> Test Report</h1>\n")
        buffWriter.write("<div style=\"width: 100%; position: relative;display:flex; flex-direction:column;justify-content:center\">")

        mapClassToTestName.each{entry ->

            buffWriter.write("    <div style=\"width: 100%; position: relative; margin-top:2em\">\n")
            buffWriter.write("        <h2  onClick=\"toggleVisibility('${entry.key}')\" style=\"font-size:1.3rem; background-color:lightgrey; width:85%;margin:0px 0px 0px 5%;padding:5px 30px 5px;  border-left: solid 15px #93e078; cursor:pointer\">${entry.key}</h2>\n")
            buffWriter.write("        <ul id=\"${entry.key}\" style=\"position:relative;display:inline-block;  list-style: none; width:85%;margin:0px 0px 0px 5%;\">\n")

            def list=entry.value
            list.unique(false).each {

                long time = 0

                mapTestNameToTestsRunResult[it] .each {i ->
                    time+=i.EndTime - i.StartTime

                }

                mapTestNameToTestsRunResult[it].sort{a, b -> b.StartTime-a.StartTime}
                def lastExecuted= mapTestNameToTestsRunResult[it][0]

                buffWriter.write("            <li onClick=\"toggleVisibility('${it}')\" style=\"display:inline-block;position:relative; width:100%;border-bottom: solid 1px #dbdbdb;  padding: .5em 0 .5em 1em; border-left: solid 7px #93e078; cursor:pointer;\">\n")
                buffWriter.write("<span style=\"display:block; float:left; \">${lastExecuted.name}</span>")

                //To convert time in ms to appropriate form
                String timeInString = "";

                if (time >= hourInMs) {
                    timeInString = (long) (time / hourInMs) + "hr";
                    time = time % hourInMs;
                    if (time >= timeInMin) {
                        timeInString += time / timeInMin + "min";
                        time = time % timeInMin;
                    }
                } else if (time >= timeInMin) {
                    timeInString += time / timeInMin + "min";
                    time = time % timeInMin;
                } else if (time >= 1000) {
                    timeInString += time / 1000 + "s";

                } else {
                    timeInString = time + "ms";
                }

                buffWriter.write("<span style=\"display:block; float:right; margin-right: 1em; width:5em\">${timeInString} </span>")
                if (lastExecuted.result == "SUCCESS") {
                    buffWriter.write("<span style=\"display:block; float:right; color:#93e078; margin-right: 3em;\">${lastExecuted.result} </span>")

                } else {
                    buffWriter.write("<span style=\"display:block; float:right; color:red; margin-right: 3em;\">${lastExecuted.result} </span>")

                }
                buffWriter.write("</li>\n")



                buffWriter.write("<div id=${it} style=\"display:none;position:relative;width:100%;border-bottom: solid 1px #dbdbdb;  padding: .5em 0 .5em 1em; border-left: solid 2px #93e078; background-color:lightgrey; margin-left:1.2em; \">\n")
                buffWriter.write("<h2 style=\"font-size:1rem;\">Test Run Details</h2>\n")
                buffWriter.write("    <ul style=\"position:relative;  list-style: none; width:100%;\">\n")

                mapTestNameToTestsRunResult[it] .each {

                    Date dateStart = new Date(it.StartTime);
                    format = new SimpleDateFormat("HH:mm:ss   yyyy/MM/dd");
                    format.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
                    String startTime = format.format(dateStart);
                    time = it.EndTime - it.StartTime
                    timeInString = "";

                    if (time >= hourInMs) {
                        timeInString = (long) (time / hourInMs) + "hr";
                        time = time % hourInMs;
                        if (time >= timeInMin) {
                            timeInString += time / timeInMin + "min";
                            time = time % timeInMin;
                        }
                    } else if (time >= timeInMin) {
                        timeInString += time / timeInMin + "min";
                        time = time % timeInMin;
                    } else if (time >= 1000) {
                        timeInString += time / 1000 + "s";
                    } else {
                        timeInString = time + "ms";

                    }



                    buffWriter.write(" <li style=\"display:inline-block;position:relative; width:100%; margin-top:10px;\">\n" +
                                "           <span style=\"display:block; float:left; \">${startTime}</span>\n" +
                                "           <span style=\"display:block; float:right;  margin-right:5%; width:150px;\">Duration:${timeInString} </span>\n" )


                   if(it.result =="FAILURE") {
                       buffWriter.write("           <span style=\"display:block; float:left;color:white; background-color:red; margin-left:2%; font-size:16px; padding:4px; border-radius:10%;\">FAILURE </span>\n" +
                                       "           <span style=\"display:block; float:left; color:red; margin-left:2%;\">${it.ErrorDescription}</span>\n" +
                                       "        </li>\n")


                   } else{
                       buffWriter.write("           <span style=\"display:block; float:left;color:white; background-color:#93e078; margin-left:2%; font-size:16px; padding:4px; border-radius:10%;\">${it.result} </span>\n" +
                                        "        </li>\n")
                    }

                }
                buffWriter.write("</ul>\n")
                buffWriter.write("</div>\n")

            }
            buffWriter.write("</ul>\n")
            buffWriter.write("</div>\n")
        }
        buffWriter.write("</div>\n")
        buffWriter.write("</div>\n")




        //------------------Test OwnerShip----------------------
        buffWriter.write("     <div id=\"Test-Ownership-container\" style=\"display:none;width: 100%; position: relative;\">\n")
        buffWriter.write("<h1> Test OwnerShip Report</h1>\n")
        buffWriter.write("<div style=\"width: 100%; position: relative;display:flex; flex-direction:column;justify-content:center\">")

        mapOwnerToTestName.each{entry ->


            buffWriter.write("    <div style=\"width: 100%; position: relative; margin-top:2em\">\n")
            buffWriter.write("        <h2  onClick=\"toggleVisibility('${entry.key}')\" style=\"font-size:1.3rem; background-color:lightgrey; width:85%;margin:0px 0px 0px 5%;padding:5px 30px 5px;  border-left: solid 15px #93e078; cursor:pointer\">${entry.key}</h2>\n")
            buffWriter.write("        <ul id=\"${entry.key}\" style=\"position:relative;display:inline-block;  list-style: none; width:85%;margin:0px 0px 0px 5%;\">\n")

            def list=entry.value
            list.unique(false).each {
                mapTestNameToTestsRunResult[it].sort{a, b -> b.StartTime-a.StartTime}
                def lastExecuted= mapTestNameToTestsRunResult[it][0]

                buffWriter.write("            <li onClick=\"toggleVisibility('${it}.${entry.key}')\" style=\"display:inline-block;position:relative; width:100%;border-bottom: solid 1px #dbdbdb;  padding: .5em 0 .5em 1em; border-left: solid 7px #93e078; cursor:pointer;\">\n")
                buffWriter.write("                    <span style=\"display:block; float:left; \">${lastExecuted.name}(${lastExecuted.className})</span>")
                long time = 0

                mapTestNameToTestsRunResult[it] .each {i ->
                    time+=i.EndTime - i.StartTime

                }
                String timeInString = "";

                if (time >= hourInMs) {
                    timeInString = (long) (time / hourInMs) + "hr";
                    time = time % hourInMs;
                    if (time >= timeInMin) {
                        timeInString += time / timeInMin + "min";
                        time = time % timeInMin;
                    }
                } else if (time >= timeInMin) {
                    timeInString += time / timeInMin + "min";
                    time = time % timeInMin;
                } else if (time >= 1000) {
                    timeInString += time / 1000 + "s";
                } else {
                    timeInString = time + "ms";

                }

                buffWriter.write("<span style=\"display:block; float:right; margin-right: 1em; width:5em\">${timeInString} </span>")
                if (lastExecuted.result == "SUCCESS") {
                    buffWriter.write("<span style=\"display:block; float:right; color:#93e078; margin-right: 3em;\">${lastExecuted.result} </span>")

                } else {
                    buffWriter.write("<span style=\"display:block; float:right; color:red; margin-right: 3em;\">${lastExecuted.result} </span>")

                }
                buffWriter.write("</li>\n")



                buffWriter.write("<div id=${it}.${entry.key} style=\"display:none;position:relative;width:100%;border-bottom: solid 1px #dbdbdb;  padding: .5em 0 .5em 1em; border-left: solid 2px #93e078; background-color:lightgrey; margin-left:1.2em; \">\n")
                buffWriter.write("<h2 style=\"font-size:1rem;\">Test Run Details</h2>\n")
                buffWriter.write("    <ul style=\"position:relative;  list-style: none; width:100%;\">\n")
                mapTestNameToTestsRunResult[it] .each {

                    Date dateStart = new Date(it.StartTime);
                    format = new SimpleDateFormat("HH:mm:ss   yyyy/MM/dd");
                    format.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
                    String startTime = format.format(dateStart);
                    time = it.EndTime - it.StartTime
                    timeInString = "";

                    if (time >= hourInMs) {
                        timeInString = (long) (time / hourInMs) + "hr";
                        time = time % hourInMs;
                        if (time >= timeInMin) {
                            timeInString += time / timeInMin + "min";
                            time = time % timeInMin;
                        }
                    } else if (time >= timeInMin) {
                        timeInString += time / timeInMin + "min";
                        time = time % timeInMin;
                    } else if (time >= 1000) {
                        timeInString += time / 1000 + "s";
                    } else {
                        timeInString = time + "ms";

                    }



                    buffWriter.write(" <li style=\"display:inline-block;position:relative; width:100%; margin-top:10px;\">\n" +
                            "           <span style=\"display:block; float:left; \">${startTime}</span>\n" +
                            "           <span style=\"display:block; float:right;  margin-right:5%; width:150px;\">Duration:${timeInString} </span>\n" )


                    if(it.result =="FAILURE") {
                        buffWriter.write("           <span style=\"display:block; float:left;color:white; background-color:red; margin-left:2%; font-size:16px; padding:4px; border-radius:10%;\">FAILURE </span>\n" +
                                        "           <span style=\"display:block; float:left; color:red; margin-left:2%;\">${it.ErrorDescription}</span>\n" +
                                        "        </li>\n")


                    }                    else{
                        buffWriter.write("           <span style=\"display:block; float:left;color:white; background-color:#93e078; margin-left:2%; font-size:16px; padding:4px; border-radius:10%;\">${it.result} </span>\n" +
                                        "        </li>\n")
                    }

                }
                buffWriter.write("</ul>\n")
                buffWriter.write("</div>\n")

            }
            buffWriter.write("</ul>\n")
            buffWriter.write("</div>\n")
        }
        buffWriter.write("</div>\n")
        buffWriter.write("</div>\n")

        buffWriter.write("</body>\n")
        buffWriter.write("</html>\n")
        buffWriter.flush()
        buffWriter.close()

    }


}
