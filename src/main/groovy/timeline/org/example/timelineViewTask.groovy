package timeline.org.example

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Project;
import javax.naming.spi.ObjectFactory
import java.text.DateFormat
import java.text.SimpleDateFormat

public class timelineViewTask extends DefaultTask{

    @TaskAction
    def createHtmlReport () {


                File reportfol = new File("build/testResult/Report")
                reportfol.mkdir()
                File report = new File("build/testResult/Report/myReport.html")


                report.write("<html>\n")
                report.append("<head>\n")
                report.append("     <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n")
                report.append("     <script type=\"text/javascript\">\n")
                report.append("      google.charts.load('current', {'packages':['timeline']});\n")
                report.append("      google.charts.setOnLoadCallback(drawChart);\n")
                report.append("      function drawChart() {\n")
                report.append("        var container = document.getElementById('timeline');\n")
                report.append("        var chart = new google.visualization.Timeline(container);\n")
                report.append("        var dataTable = new google.visualization.DataTable();\n")
                report.append("        dataTable.addColumn({ type: 'string', id: 'Thread' });\n")
                report.append("        dataTable.addColumn({ type: 'string', id: 'Name' });\n")
                report.append("        dataTable.addColumn({ type: 'date', id: 'Start' });\n")
                report.append("        dataTable.addColumn({ type: 'date', id: 'End' });\n")
                report.append("    dataTable.addRows([\n")






                def JSON_files =  project.file("${project.rootDir}/build/testResult").listFiles().sort()
                def map  = [:]
                JSON_files.each { File file ->
                    if (file.isFile()) {
                        Scanner myScanner = new Scanner(file);

                        String result_JSON = myScanner.nextLine();
                        def parsedJson_result = new groovy.json.JsonSlurper().parseText(result_JSON)
                        def list = []
                        if(map.containsKey(parsedJson_result.className)){
                            list=map[parsedJson_result.className]
                        }
                        list.add(parsedJson_result)
                        map[parsedJson_result.className]=list




                        Date dateStart = new Date(parsedJson_result.StartTime);
                        DateFormat format = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
                        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
                        String startTime = format.format(dateStart);
                        Date dateEnd = new Date(parsedJson_result.EndTime);
                        String endTime = format.format(dateEnd);

                        if(endTime.charAt(20)=='0'){
                            endTime=endTime.substring(0,20)+endTime.substring(21)

                        }

                        if(startTime.charAt(20)=='0'){

                            startTime=startTime.substring(0,20)+startTime.substring(21);

                        }
                        report.append("      [ '${parsedJson_result.Thread}', '${parsedJson_result.name}', new Date(${startTime}), new Date(${endTime}) ],\n")


                    }
                }

                report.append("]);\n")
                report.append("    var options = {\n")
                report.append("      timeline: { showRowLabels: false, singleColor: '#93e078' },alternatingRowStyle:false\n")
                report.append("    };\n")
                report.append("    chart.draw(dataTable,options);\n")
                report.append("  }\n")


                report.append("function report_decrease_timelinezoom () {\n" +
                        "              const item=document.getElementById('timeline');\n" +
                        "            var initial=parseInt(item.style.width.substring(0,3))-100\n" +
                        "          if(initial>=100){\n" +
                        "           item.style.width=initial+\"%\";\n" +
                        "           console.log(item.style.width)\n" +
                        "           drawChart();\n" +
                        "         }\n" +
                        "    }\n");
                report.append("  function report_increase_timelinezoom () {\n" +
                        "       const item=document.getElementById('timeline');\n" +
                        "          var initial=parseInt(item.style.width.substring(0,3))+100\n" +
                        "           item.style.width=initial+\"%\";\n" +
                        "           console.log(item.style.width)\n" +
                        "           drawChart();\n" +
                        "    }\n")

                report.append(" </script>\n")
                report.append("</head>\n")






                report.append("<body>\n")


                report.append("<h1> Test Timeline View</h1>\n")
                report.append("<div style=\"width: 97%; position: relative; margin:auto; \">\n")
                report.append("    <button onClick=\"report_decrease_timelinezoom()\"  style=\" position: relative;float:right; cursor:pointer; font-size:1.5rem; border: solid 2px #dbdbdb; padding:0px 7px;\">-</button>\n")
                report.append("     <button onClick=\"report_increase_timelinezoom()\"  style=\" position: relative;float:right; cursor:pointer;font-size:1.5rem; border: solid 2px #dbdbdb; padding:0px 5px;\">+</button>\n")
                report.append("<div style=\"width: 100%; position: relative; margin:auto; overflow-x: scroll;overflow-y:hidden;\">\n")
                report.append("<div id=\"timeline\" style=\"width:100%;  height:${100+40*map.size()}px\"></div>\n")
                report.append("</div>\n")
                report.append("</div>\n")



                report.append("<h1> Test Report</h1>\n")
                report.append("<div style=\"width: 100%; position: relative;display:flex; flex-direction:column;justify-content:center\">")
                map.each{entry ->


                    report.append("    <div style=\"width: 100%; position: relative; margin-top:2em\">\n")
                    report.append("        <h2 style=\"font-size:1.3rem; background-color:lightgrey; width:85%;margin:0px 0px 0px 5%;padding:5px 30px 5px;  border-left: solid 15px #93e078;\">${entry.key}</h2>\n")
                    report.append("        <ul style=\"position:relative;display:inline-block;  list-style: none; width:85%;margin:0px 0px 0px 5%;\">\n")

                    def list=entry.value

                    list.each {

                        report.append("            <li style=\"display:inline-block;position:relative; width:100%;border-bottom: solid 1px #dbdbdb;  padding: .5em 0 .5em 1em; border-left: solid 7px #93e078;\">\n")
                        report.append("<span style=\"display:block; float:left; \">${it.name}</span>")
                        long time=it.EndTime-it.StartTime

                        String timeInString="";
                        long hourInMs=60*60*1000;
                        long timeInMin=60*1000;
                        if(time>=hourInMs){
                            timeInString=(long)(time/hourInMs)+"hr";
                            time=time%hourInMs;
                            if(time>=timeInMin){
                                timeInString+=time/timeInMin+"min";
                                time=time%timeInMin;
                            }
                        }
                        else if(time>=timeInMin){
                                timeInString+=time/timeInMin+"min";
                                time=time%timeInMin;
                            }

                        else  if(time>=1000){
                            timeInString+=time/1000+"s";

                        }
                        else{

                            timeInString=time+"ms";

                        }

                        report.append("<span style=\"display:block; float:right; margin-right: 1em; width:5em\">${timeInString} </span>")
                        if(it.result=="SUCCESS"){
                            report.append("<span style=\"display:block; float:right; color:#93e078; margin-right: 3em;\">${it.result} </span>")

                        }
                        else {
                            report.append("<span style=\"display:block; float:right; color:red; margin-right: 3em;\">${it.result} </span>")

                        }



                        report.append("</li>\n")
                    }
                    report.append("</ul>\n")
                    report.append("</div>\n")
                }
                report.append("</div>\n")

                report.append("</body>\n")
                report.append("</html>\n")
            }


    }
