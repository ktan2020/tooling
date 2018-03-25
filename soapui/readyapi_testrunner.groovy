
@Grapes([
    @Grab(group='org.slf4j', module='slf4j-api', version='1.7.25'),
    @Grab(group='org.slf4j', module='slf4j-simple', version='1.7.25'),
])
import org.slf4j.*


NAME = null
DIR_NAME = null
LOGS_DIR = "SOAPUI_LOGS"
JUNIT_REPORT="TESTS-TestSuites.xml"
ANT = "ant"
JAVA = "java"

TEST_COUNT = 1
STATUS_CODES = []
MAX_RETRY_COUNT = 0
DRY_RUN = false

TESTSUITE_DELIMITER = "## "
TESTCASE_DELIMITER = "@@ "


def logger = LoggerFactory.getLogger(this.class)


def enumerate_testsuite_testcases_from_xml(xml_file) {
    def proj = new XmlSlurper().parse(xml_file).declareNamespace([con: 'http://eviware.com/soapui/config'])
    def M = [:]
    for (ts in proj.testSuite) {
        if (ts.@disabled != 'true') {
            M[ts.@name] = []            
            for (tc in ts.testCase) { 
                if (tc.@disabled != 'true') {
                    M[ts.@name] << tc.@name
                } 
            }
        }
    }
    M 
}


def in_path(exe_name) {
    System.getenv('PATH').split(':').grep { new File(it + "/" + exe_name).isFile() } != []
}


def exec(command) {
    println "[*] Running the following command in DIR_NAME=[$DIR_NAME]: "
    println "Invoking: ***$command***"

    if (DRY_RUN) return new Random().nextInt(2) // only for dry-run mode randomly return 0|1
    
    def p = (DIR_NAME) ? command.execute(null, new File(DIR_NAME)) : command.execute()
    p.waitForProcessOutput(System.out, System.err)
    p.exitValue()
}


def gen_report() {
    if (!DRY_RUN && in_path("ant")) {    
        // Generate an aggregated junit xml report     
        def ant = new AntBuilder()
        BUILD_XML = """
<project name="report" default="report" basedir=".">
    <description>
        Generate the HTML report from JUnit XML files
    </description>

    <target name="report">
        <junitreport todir="${LOGS_DIR}">
            <fileset dir="${LOGS_DIR}" includes="**/TEST-*.xml"/>
            
            <report format="frames" todir="${LOGS_DIR}">
                <param name="TITLE" expression="SoapUI Test Results."/>
            </report>
        </junitreport>
    </target>
</project>
"""
        ant.echo(file:'build.xml', BUILD_XML)
        def p = "ant".execute()    
        p.waitFor()
    } else {
        logger.warn("!!! ${ANT} not in PATH. NOT generating aggregate report !!!")
    }
}


def params_to_tuple(params) {
    tuple = []
    params.split(TESTSUITE_DELIMITER).each {
        it = it.trim()
        if (it!='') {
            def suitename = ""
            def _ = it.split(",")
            
            if (!_[0].startsWith(TESTCASE_DELIMITER)) { 
                suitename = _[0]
                _ = (_.size() > 1) ? _[1..-1] : _
            }
    
            if (_.every { it.startsWith(TESTCASE_DELIMITER) }) {
                _ = _.collect { it.replace(TESTCASE_DELIMITER,"")}
                tuple << [suitename, _]
            } else {
                tuple << [suitename,[]]
            }
        }
    }
    tuple
}


def cli = new CliBuilder(usage: "${this.class.name} [options] <soapui_project.xml> [optional testsuite / testcase names] <env_name>")
cli.h(longOpt:'help', "Print this message")
cli.r(args: 1, longOpt: "retry", optionalArg: true, argName: "max_retry_count", "Rerun failed test(s) N up to number of times. Default is 0")
cli.d(longOpt: 'dry-run', "Dry run flag. If 'dry-run' set to 'true' we skip invoking 'command' in the 'exec' function. Default is 'false'")

def options = cli.parse(args)

if (!options || options.h) {
    cli.usage()
    System.exit(1)
} else {

    def PROJECT_NAME = options.arguments()[0] // project xml is always the 1st param 
    def PARAMS = options.arguments() ? options.arguments().tail() : null   // rest of the list will be params
    
    MAX_RETRY_COUNT = (options.r) ? (options.r as int) : MAX_RETRY_COUNT
    DRY_RUN = options.d ?: DRY_RUN
    
    logger.debug "PARAMS: $PARAMS"
    
    def (TESTS, ENV_NAME) = ["",""] 


    if (PARAMS != []) {
        // testsuite name(s) and/or testcase name(s) in predetermined format 
        for (_ in PARAMS) {
             if (_.contains(TESTSUITE_DELIMITER) || _.contains(TESTCASE_DELIMITER)) {
                 TESTS = _
             } else {
                 ENV_NAME = _
             }
        }
        
        logger.debug "TESTS: $TESTS, ENV_NAME: $ENV_NAME"
     
        if (TESTS) {
            TESTS = params_to_tuple(TESTS)
            logger.debug "---> TESTS TUPLE: $TESTS <---"
        }
    } 
    
    def SUITE = (TESTS.size() > 0) ? TESTS[0][0] : []
    def CASES = (TESTS.size() > 0) ? TESTS[0][1] : []

    if (CASES.size() < 1 && SUITE.size() < 1) {
        logger.warn "!!! Running the entire project XML !!!!"
    }

    println "===> ENV_NAME: $ENV_NAME, TEST_SUITE_NAME: $SUITE, TEST_CASE_NAMES: $CASES <===" 


    def EXT_LIB = null
    def SETTINGS_XML = null
    
    
    if (PROJECT_NAME?.endsWith(".xml")) {
        DIR_NAME  = (new File(PROJECT_NAME)).absoluteFile.parent
        NAME = (new File(PROJECT_NAME)).name
    } else {
        println "XXX FATAL ERROR: Soapui project parameter must be an XML! XXX"
        System.exit(1)
    }
    
    // check for existance of jars folder in soapui project dir
    if (new File("${DIR_NAME}/jars").exists() && new File("${DIR_NAME}/jars").isDirectory()) {
        EXT_LIB = "${DIR_NAME}/jars"
        logger.info("Setting EXT_LIB to [$EXT_LIB]")
    } else {
        logger.warn("!!! EXT_LIB not set !!!")
    }
    
    // check for existance of conf/soapui-settings.xml file in project dir
    if (new File("${DIR_NAME}/conf/soapui-settings.xml").exists() && new File("${DIR_NAME}/conf/soapui-settings.xml").isFile()) {
        SETTINGS_XML = "${DIR_NAME}/conf/soapui-settings.xml"
        logger.info("Setting SETTINGS_XML to [$SETTINGS_XML]")
    } else {
        logger.warn("!!! SETTINGS_XML not set. No user-provided soapui-settings.xml was found !!!")
    }
    
    
    def READY_API_HOME = System.getenv("READY_API_HOME")
    def READY_API_CLASSPATH = "${READY_API_HOME}/bin/ready-api-ui-1.9.0.jar:${READY_API_HOME}/lib/*"
    
    def JFXRTPATH = "$JAVA -cp $READY_API_CLASSPATH com.eviware.soapui.tools.JfxrtLocator".execute().text.trim().replaceAll("(\n|\r)","")
    def READY_PERM_SIZE = "$JAVA -cp $READY_API_CLASSPATH com.eviware.soapui.tools.PermSizeCalculator".execute().text.trim().replaceAll("(\n|\r)","")
    def READY_XMX = "$JAVA -cp $READY_API_CLASSPATH com.eviware.soapui.tools.XmxCalculator".execute().text.trim().replaceAll("(\n|\r)","")
    READY_API_CLASSPATH = "${JFXRTPATH}${READY_API_CLASSPATH}"

    
    // display readyapi vars ... 
    logger.info("READY_API_HOME => [$READY_API_HOME]")
    logger.info("READY_API_CLASSPATH => [$READY_API_CLASSPATH]")
    logger.info("JFXRTPATH => [$JFXRTPATH]")
    logger.info("READY_PERM_SIZE => [$READY_PERM_SIZE]")
    logger.info("READY_XMX => [$READY_XMX]")
    
    
    def JAVA_OPTS = ["-Xms128m", "-Xmx$READY_XMX", "-Dtest.history.disabled=true", "-Dsoapui.properties=soapui.properties", "-Dgroovy.source.encoding=iso-8859-1", "-Dsoapui.home=$READY_API_HOME/bin"]
    JAVA_OPTS << "-Dsoapui.ext.libraries=${EXT_LIB}" 
    JAVA_OPTS << "-Dsoapui.ext.listeners=$READY_API_HOME/bin/listeners"
    JAVA_OPTS << "-Dsoapui.ext.actions=$READY_API_HOME/bin/actions"

    // pickup parent process' JAVA_OPTS env var
    JAVA_OPTS += System.getenv('JAVA_OPTS').split('-D').grep().collect {"-D" + it} 

    logger.info("JAVA_OPTS => $JAVA_OPTS")
    

    def INVOKE = ["$JAVA"] + JAVA_OPTS + ["-cp", READY_API_CLASSPATH, "com.smartbear.ready.cmd.runner.pro.SoapUIProTestCaseRunner"]
    INVOKE << "$NAME" << "-O" << "-r" << "-a" << "-j" << "-M" << "-FXML" << '-R"JUnit-Style HTML Report"' 
    
    
    if (SETTINGS_XML) {
        INVOKE << "-t" << SETTINGS_XML 
    }
    if (ENV_NAME && ENV_NAME!='') {
        INVOKE << "-E" << ENV_NAME
    }
    if (SUITE!=[] && SUITE!="") {
        INVOKE << "-s" << SUITE    // XXX TBD 
    } 
    if (CASES!=[]) {
        // testcases specified
        CASES.each {
            println "+" * 80
            println "Running ${TEST_COUNT} / ${CASES.size()} : <${it}>"
            println "+" * 80
            
            _ = INVOKE.collect()
    
            _ << "-c" << it << "-f" << (LOGS_DIR + File.separatorChar + TEST_COUNT + "_" + it.replaceAll(" ", "_"))  // XXX TBD
    
            logger.info(" => exec string: " + _)
            ret = exec(_)
    
            STATUS_CODES.push(ret)
        
            TEST_COUNT++
        }        
    } else {
        // no testcase(s) specified so run suite
        _  = INVOKE.collect()
        
        _ << "-f" << LOGS_DIR

        logger.info(" => exec string: " + _)
        ret = exec(_)
    
        STATUS_CODES.push(ret)
    }
    
    println "*** STATUS_CODES: " + STATUS_CODES + " ***"
    gen_report()
    
    
    // rerun failed test(s). cap to 10x 
    if (MAX_RETRY_COUNT > 0 && MAX_RETRY_COUNT <= 10) {
        
        if (INVOKE.grep(~/^-s$/)) {
            // suite name was specified in cli
            INVOKE = INVOKE.dropRight(2)
            logger.debug("@@@ INVOKE cmd: ${INVOKE} @@@")
        } 
     
        def testsuites
        def (rerun_suitenames, rerun_testcases) = [[], [:]]
     
        if (! new File("${LOGS_DIR}/${JUNIT_REPORT}").exists()) {
            logger.warn("!!! ${JUNIT_REPORT} not found. Nothing to rerun !!!")
        } else {
            
            testsuites = new XmlParser().parse(new File("${LOGS_DIR}/${JUNIT_REPORT}"))
            
            failed_ts_nodes = testsuites.testsuite.findAll { it.'@failures' != '0' }
            logger.info(" ====%%%%---->>>> rerunning the following testsuites: ${failed_ts_nodes*.@name} <<<<----%%%%==== ")
            
            rerun_testcases = failed_ts_nodes.inject([:]) { m, node -> 
                // check to make sure # of testsuite failures attribute matches # of testcase failure nodes
                assert (node.@failures as int) == node.testcase.findAll { it.failure }.size()
                m[node.@name] = node.testcase.findAll { it.failure }*.@name
                m
            }
            
            logger.info(" ----%%%%====>>>> rerun testcases dictionary: ${rerun_testcases} <<<<%%%%====---- ")
            
            if (! rerun_testcases) {
                logger.info(" @@@ ### No failed tests! ᕙ(⇀‸↼‶)ᕗ  ### @@@ ")
            } else {
                // reset the STATUS_CODES only for failing testsuites & testcases
                STATUS_CODES = [] 
             
                rerun_testcases.each { testsuite, testcases_list ->
                    
                    if (testsuite.lastIndexOf(".") != -1) {
                        // strip project name from testsuite name
                        testsuite = testsuite.substring(testsuite.lastIndexOf(".")+1)
                    }
                
                    boolean tc_pass = false
                    for (def testcase in testcases_list) {
                        int retry_count 
                        
                        for (retry_count=1; retry_count<=MAX_RETRY_COUNT; retry_count++) {
                          
                            _ = INVOKE.collect()
                            
                            _ << "-s" << testsuite <<
                                 "-c" << testcase <<
                                 "-f" << "${LOGS_DIR}/RETRY_${testsuite.replaceAll(' ','_')}/${testcase.replaceAll(' ','_')}_retry#${retry_count}"
                            
                            logger.info(" [${retry_count}/${MAX_RETRY_COUNT}] => exec string: " + _)
                                                 
                            int ret = exec(_)
                            if (ret==0) {
                                logger.info(" !!!!! ##### ${testsuite}-${testcase} passed on ${retry_count}/${MAX_RETRY_COUNT} ##### !!!!! ")
                                
                                tc_pass = true
                                break
                            }
                            
                            sleep(2_000 * retry_count)
                        }
                        
                        if (tc_pass) {
                            STATUS_CODES.push(0)
                        } else if (retry_count > MAX_RETRY_COUNT) {
                            STATUS_CODES.push(1)
                        }
                        
                        sleep(1_000)
                    }
                }
            }
        }     
        
        println "&&& RETRY STATUS_CODES: " + STATUS_CODES + " &&&"
        gen_report()   
    }
    // End of retry logic
    

    STATUS_CODES!=[] && STATUS_CODES.every { it==0 } ? System.exit(0) : System.exit(1) // return PASS only if all status codes are pass
} 
